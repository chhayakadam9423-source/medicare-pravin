-- ============================================================
-- MEDICARE — HOSPITAL MANAGEMENT SYSTEM
-- SUPABASE POSTGRESQL DATABASE SCHEMA & ROW LEVEL SECURITY
-- ============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. PROFILES TABLE (Linked to auth.users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    email TEXT UNIQUE,
    phone TEXT,
    role TEXT NOT NULL CHECK (role IN ('patient', 'doctor', 'admin')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 2. DOCTORS TABLE
CREATE TABLE IF NOT EXISTS public.doctors (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    specialization TEXT NOT NULL,
    qualification TEXT NOT NULL,
    experience_years TEXT NOT NULL,
    license_number TEXT,
    hospital_name TEXT,
    department TEXT,
    consultation_fee NUMERIC,
    available_days TEXT,
    start_time TEXT,
    end_time TEXT,
    bio TEXT,
    profile_image_url TEXT,
    available BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 3. PATIENTS TABLE
CREATE TABLE IF NOT EXISTS public.patients (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    date_of_birth DATE,
    gender TEXT CHECK (gender IN ('Male', 'Female', 'Other')),
    blood_group TEXT CHECK (blood_group IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 4. APPOINTMENTS TABLE
CREATE TABLE IF NOT EXISTS public.appointments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id UUID NOT NULL REFERENCES public.patients(id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES public.doctors(id) ON DELETE CASCADE,
    appointment_date DATE NOT NULL,
    appointment_time TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'confirmed', 'rejected', 'completed', 'cancelled')),
    reason TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 5. PRESCRIPTIONS TABLE
CREATE TABLE IF NOT EXISTS public.prescriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    appointment_id UUID NOT NULL REFERENCES public.appointments(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES public.patients(id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES public.doctors(id) ON DELETE CASCADE,
    medicine TEXT NOT NULL,
    dosage TEXT NOT NULL,
    frequency TEXT NOT NULL,
    instructions TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- ============================================================
-- INDEXES FOR HIGH QUERY PERFORMANCE
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_profiles_role ON public.profiles(role);
CREATE INDEX IF NOT EXISTS idx_doctors_profile ON public.doctors(profile_id);
CREATE INDEX IF NOT EXISTS idx_doctors_specialization ON public.doctors(specialization);
CREATE INDEX IF NOT EXISTS idx_patients_profile ON public.patients(profile_id);
CREATE INDEX IF NOT EXISTS idx_appointments_patient ON public.appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor ON public.appointments(doctor_id);
CREATE INDEX IF NOT EXISTS idx_appointments_date ON public.appointments(appointment_date);
CREATE INDEX IF NOT EXISTS idx_appointments_status ON public.appointments(status);
CREATE INDEX IF NOT EXISTS idx_prescriptions_appointment ON public.prescriptions(appointment_id);
CREATE INDEX IF NOT EXISTS idx_prescriptions_patient ON public.prescriptions(patient_id);

-- ============================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.doctors ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.patients ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.appointments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.prescriptions ENABLE ROW LEVEL SECURITY;

-- Helper functions for role checking
CREATE OR REPLACE FUNCTION public.current_user_role()
RETURNS TEXT AS $$
    SELECT role FROM public.profiles WHERE id = auth.uid();
$$ LANGUAGE sql STABLE SECURITY DEFINER;

-- PROFILES POLICIES
CREATE POLICY "Users can view own profile or all users if authenticated"
ON public.profiles FOR SELECT
TO authenticated
USING (true);

CREATE POLICY "Users can update own profile"
ON public.profiles FOR UPDATE
TO authenticated
USING (auth.uid() = id);

CREATE POLICY "Users can insert own profile"
ON public.profiles FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = id);

CREATE POLICY "Admins can manage all profiles"
ON public.profiles FOR ALL
TO authenticated
USING (public.current_user_role() = 'admin');

-- DOCTORS POLICIES
CREATE POLICY "Anyone authenticated can view doctors"
ON public.doctors FOR SELECT
TO authenticated
USING (true);

CREATE POLICY "Doctors can update their own doctor record"
ON public.doctors FOR UPDATE
TO authenticated
USING (profile_id = auth.uid());

CREATE POLICY "Admins can insert, update, delete doctors"
ON public.doctors FOR ALL
TO authenticated
USING (public.current_user_role() = 'admin');

-- PATIENTS POLICIES
CREATE POLICY "Patients can view and update own patient record"
ON public.patients FOR ALL
TO authenticated
USING (
    profile_id = auth.uid() 
    OR public.current_user_role() IN ('doctor', 'admin')
);

-- APPOINTMENTS POLICIES
CREATE POLICY "Patients can select own appointments"
ON public.appointments FOR SELECT
TO authenticated
USING (
    patient_id IN (SELECT id FROM public.patients WHERE profile_id = auth.uid())
    OR doctor_id IN (SELECT id FROM public.doctors WHERE profile_id = auth.uid())
    OR public.current_user_role() = 'admin'
);

CREATE POLICY "Patients can create appointments"
ON public.appointments FOR INSERT
TO authenticated
WITH CHECK (
    patient_id IN (SELECT id FROM public.patients WHERE profile_id = auth.uid())
);

CREATE POLICY "Patients and Doctors can update appointments"
ON public.appointments FOR UPDATE
TO authenticated
USING (
    patient_id IN (SELECT id FROM public.patients WHERE profile_id = auth.uid())
    OR doctor_id IN (SELECT id FROM public.doctors WHERE profile_id = auth.uid())
    OR public.current_user_role() = 'admin'
);

-- PRESCRIPTIONS POLICIES
CREATE POLICY "Patients and Doctors can view relevant prescriptions"
ON public.prescriptions FOR SELECT
TO authenticated
USING (
    patient_id IN (SELECT id FROM public.patients WHERE profile_id = auth.uid())
    OR doctor_id IN (SELECT id FROM public.doctors WHERE profile_id = auth.uid())
    OR public.current_user_role() = 'admin'
);

CREATE POLICY "Doctors can create prescriptions"
ON public.prescriptions FOR INSERT
TO authenticated
WITH CHECK (
    doctor_id IN (SELECT id FROM public.doctors WHERE profile_id = auth.uid())
    OR public.current_user_role() = 'admin'
);

-- ============================================================
-- TRIGGER: Automatically create profile on Supabase auth signup
-- ============================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, name, email, phone, role)
    VALUES (
        new.id,
        COALESCE(new.raw_user_meta_data->>'name', 'User'),
        new.email,
        COALESCE(new.raw_user_meta_data->>'phone', ''),
        COALESCE(new.raw_user_meta_data->>'role', 'patient')
    );

    IF (new.raw_user_meta_data->>'role' = 'patient') THEN
        INSERT INTO public.patients (profile_id, gender, blood_group)
        VALUES (new.id, 'Other', 'O+');
    END IF;

    IF (new.raw_user_meta_data->>'role' = 'doctor') THEN
        INSERT INTO public.doctors (
            profile_id, specialization, qualification, experience_years,
            license_number, hospital_name, department, consultation_fee,
            available_days, start_time, end_time, bio, available
        )
        VALUES (
            new.id, 
            COALESCE(new.raw_user_meta_data->>'specialization', 'General Medicine'),
            COALESCE(new.raw_user_meta_data->>'qualification', 'MBBS, MD'),
            COALESCE(new.raw_user_meta_data->>'experience', '5+ Years'),
            COALESCE(new.raw_user_meta_data->>'license_number', ''),
            COALESCE(new.raw_user_meta_data->>'hospital_name', ''),
            COALESCE(new.raw_user_meta_data->>'department', ''),
            NULLIF(new.raw_user_meta_data->>'consultation_fee', '')::NUMERIC,
            COALESCE(new.raw_user_meta_data->>'available_days', ''),
            COALESCE(new.raw_user_meta_data->>'start_time', ''),
            COALESCE(new.raw_user_meta_data->>'end_time', ''),
            COALESCE(new.raw_user_meta_data->>'about', ''),
            true
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ============================================================
-- DEVELOPMENT SEED DATA (OPTIONAL RUN FOR LOCAL / DEV TESTING)
-- ============================================================

-- Profiles
INSERT INTO public.profiles (id, name, email, phone, role)
VALUES 
('d1111111-1111-1111-1111-111111111111', 'Dr. Sarah Jenkins', 'sarah.jenkins@medicare.com', '+1-555-0101', 'doctor'),
('d2222222-2222-2222-2222-222222222222', 'Dr. Michael Chen', 'michael.chen@medicare.com', '+1-555-0102', 'doctor'),
('d3333333-3333-3333-3333-333333333333', 'Dr. Priya Sharma', 'priya.sharma@medicare.com', '+1-555-0103', 'doctor'),
('d4444444-4444-4444-4444-444444444444', 'Dr. James Wilson', 'james.wilson@medicare.com', '+1-555-0104', 'doctor'),
('p1111111-1111-1111-1111-111111111111', 'Alex Johnson', 'alex.patient@medicare.com', '+1-555-0201', 'patient'),
('a1111111-1111-1111-1111-111111111111', 'Admin Supervisor', 'admin@medicare.com', '+1-555-0999', 'admin')
ON CONFLICT (id) DO NOTHING;

-- Doctors
INSERT INTO public.doctors (id, profile_id, specialization, qualification, experience, about, image_url, available)
VALUES
('11111111-0000-0000-0000-000000000001', 'd1111111-1111-1111-1111-111111111111', 'Cardiologist', 'MBBS, MD (Cardiology), FACC', '12 Years', 'Senior Consultant Interventional Cardiologist specializing in heart disease prevention, echocardiography, and hypertension control.', 'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&q=80&w=300', true),
('11111111-0000-0000-0000-000000000002', 'd2222222-2222-2222-2222-222222222222', 'Neurologist', 'MBBS, DM (Neurology)', '9 Years', 'Specialist in clinical neuroscience, stroke rehabilitation, migraine disorders, and neuromuscular diagnostics.', 'https://images.unsplash.com/photo-1622253692010-333f2da6031d?auto=format&fit=crop&q=80&w=300', true),
('11111111-0000-0000-0000-000000000003', 'd3333333-3333-3333-3333-333333333333', 'Pediatrician', 'MBBS, DCH, MD (Pediatrics)', '8 Years', 'Dedicated to child wellness, infant care, pediatric developmental monitoring, and allergy management.', 'https://images.unsplash.com/photo-1594824813580-c116c4fa31b2?auto=format&fit=crop&q=80&w=300', true),
('11111111-0000-0000-0000-000000000004', 'd4444444-4444-4444-4444-444444444444', 'Orthopedic Surgeon', 'MBBS, MS (Orthopedics)', '14 Years', 'Expert in joint replacement, sports injury trauma, spinal health, and arthroscopic surgical procedures.', 'https://images.unsplash.com/photo-1537368910025-700350fe46c7?auto=format&fit=crop&q=80&w=300', true)
ON CONFLICT (id) DO NOTHING;

-- Patients
INSERT INTO public.patients (id, profile_id, date_of_birth, gender, blood_group)
VALUES
('22222222-0000-0000-0000-000000000001', 'p1111111-1111-1111-1111-111111111111', '1995-04-12', 'Male', 'O+')
ON CONFLICT (id) DO NOTHING;

-- Appointments
INSERT INTO public.appointments (id, patient_id, doctor_id, appointment_date, appointment_time, status, reason)
VALUES
('33333333-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000001', CURRENT_DATE + INTERVAL '1 day', '10:30 AM', 'confirmed', 'Routine cardiac wellness checkup and blood pressure monitoring'),
('33333333-0000-0000-0000-000000000002', '22222222-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000002', CURRENT_DATE - INTERVAL '3 days', '02:15 PM', 'completed', 'Persistent tension headaches during work hours')
ON CONFLICT (id) DO NOTHING;

-- Prescriptions
INSERT INTO public.prescriptions (id, appointment_id, patient_id, doctor_id, medicine, dosage, frequency, instructions)
VALUES
('44444444-0000-0000-0000-000000000001', '33333333-0000-0000-0000-000000000002', '22222222-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000002', 'Naproxen Sodium 250mg & Magnesium Glycinate 200mg', '1 Tablet', 'Twice daily after meals', 'Take with plenty of water. Avoid staring at bright computer screens for extended periods.')
ON CONFLICT (id) DO NOTHING;
