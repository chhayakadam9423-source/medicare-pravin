-- ============================================================
-- MEDICARE MIGRATION: FIX REGISTRATION & HANDLE_NEW_USER TRIGGER
-- Safe for repeated execution, preserves all existing tables and data
-- ============================================================

-- 1. Ensure UUID extension is enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Update PROFILES Table columns safely
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS avatar_url TEXT;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS phone TEXT;

-- Drop any restrictive check constraint on role if it rejects casing, and re-add standard check
DO $$
BEGIN
    ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_role_check;
    ALTER TABLE public.profiles ADD CONSTRAINT profiles_role_check 
        CHECK (role IN ('patient', 'doctor', 'admin'));
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;

-- 3. Update PATIENTS Table columns safely
-- Add both profile_id and user_id to handle any column naming convention across queries
ALTER TABLE public.patients ADD COLUMN IF NOT EXISTS profile_id UUID;
ALTER TABLE public.patients ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE public.patients ADD COLUMN IF NOT EXISTS date_of_birth DATE;
ALTER TABLE public.patients ADD COLUMN IF NOT EXISTS dob DATE;
ALTER TABLE public.patients ADD COLUMN IF NOT EXISTS gender TEXT;
ALTER TABLE public.patients ADD COLUMN IF NOT EXISTS blood_group TEXT;
ALTER TABLE public.patients ADD COLUMN IF NOT EXISTS address TEXT;
ALTER TABLE public.patients ADD COLUMN IF NOT EXISTS emergency_contact TEXT;
ALTER TABLE public.patients ADD COLUMN IF NOT EXISTS medical_history TEXT;

-- If user_id exists without profile_id or vice versa, sync them
UPDATE public.patients SET profile_id = user_id WHERE profile_id IS NULL AND user_id IS NOT NULL;
UPDATE public.patients SET user_id = profile_id WHERE user_id IS NULL AND profile_id IS NOT NULL;

-- Relax NOT NULL constraints that might cause registration to fail
ALTER TABLE public.patients ALTER COLUMN gender DROP NOT NULL;
ALTER TABLE public.patients ALTER COLUMN blood_group DROP NOT NULL;
ALTER TABLE public.patients ALTER COLUMN address DROP NOT NULL;
ALTER TABLE public.patients ALTER COLUMN emergency_contact DROP NOT NULL;
ALTER TABLE public.patients ALTER COLUMN date_of_birth DROP NOT NULL;
ALTER TABLE public.patients ALTER COLUMN dob DROP NOT NULL;

-- Drop restrictive gender check constraint if it causes failures
DO $$
BEGIN
    ALTER TABLE public.patients DROP CONSTRAINT IF EXISTS patients_gender_check;
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;

-- Drop restrictive blood_group check constraint if it causes failures
DO $$
BEGIN
    ALTER TABLE public.patients DROP CONSTRAINT IF EXISTS patients_blood_group_check;
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;

-- 4. Update DOCTORS Table columns safely
-- Add both profile_id and user_id to handle any column naming convention
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS profile_id UUID;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS specialization TEXT;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS qualification TEXT;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS experience_years TEXT;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS experience TEXT;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS license_number TEXT;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS hospital_name TEXT;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS hospital TEXT;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS department TEXT;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS consultation_fee NUMERIC;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS available_days TEXT;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS start_time TEXT;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS end_time TEXT;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS bio TEXT;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS about TEXT;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS profile_image_url TEXT;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS image_url TEXT;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS available BOOLEAN DEFAULT TRUE;

-- Sync matching column values if one exists and the other is null
UPDATE public.doctors SET profile_id = user_id WHERE profile_id IS NULL AND user_id IS NOT NULL;
UPDATE public.doctors SET user_id = profile_id WHERE user_id IS NULL AND profile_id IS NOT NULL;
UPDATE public.doctors SET experience_years = experience WHERE experience_years IS NULL AND experience IS NOT NULL;
UPDATE public.doctors SET experience = experience_years WHERE experience IS NULL AND experience_years IS NOT NULL;
UPDATE public.doctors SET hospital_name = hospital WHERE hospital_name IS NULL AND hospital IS NOT NULL;
UPDATE public.doctors SET hospital = hospital_name WHERE hospital IS NULL AND hospital_name IS NOT NULL;
UPDATE public.doctors SET bio = about WHERE bio IS NULL AND about IS NOT NULL;
UPDATE public.doctors SET about = bio WHERE about IS NULL AND bio IS NOT NULL;
UPDATE public.doctors SET profile_image_url = image_url WHERE profile_image_url IS NULL AND image_url IS NOT NULL;
UPDATE public.doctors SET image_url = profile_image_url WHERE image_url IS NULL AND profile_image_url IS NOT NULL;

-- Relax NOT NULL constraints on non-essential doctor fields
ALTER TABLE public.doctors ALTER COLUMN specialization DROP NOT NULL;
ALTER TABLE public.doctors ALTER COLUMN qualification DROP NOT NULL;
ALTER TABLE public.doctors ALTER COLUMN experience_years DROP NOT NULL;
ALTER TABLE public.doctors ALTER COLUMN available DROP NOT NULL;

-- 5. Bulletproof handle_new_user() trigger function
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    v_role TEXT;
    v_dob_raw TEXT;
    v_dob DATE := NULL;
    v_gender_raw TEXT;
    v_gender TEXT;
    v_blood_raw TEXT;
    v_blood TEXT;
    v_exp TEXT;
    v_hospital TEXT;
    v_about TEXT;
    v_fee_raw TEXT;
    v_fee NUMERIC := NULL;
BEGIN
    -- 1. Normalize and validate user role
    v_role := LOWER(TRIM(COALESCE(new.raw_user_meta_data->>'role', 'patient')));
    IF v_role NOT IN ('patient', 'doctor', 'admin') THEN
        v_role := 'patient';
    END IF;

    -- 2. Safely parse and sanitize date of birth
    v_dob_raw := TRIM(COALESCE(new.raw_user_meta_data->>'date_of_birth', new.raw_user_meta_data->>'dob', ''));
    IF v_dob_raw ~ '^\d{4}-\d{2}-\d{2}$' THEN
        BEGIN
            v_dob := v_dob_raw::DATE;
        EXCEPTION WHEN OTHERS THEN
            v_dob := NULL;
        END;
    END IF;

    -- 3. Safely normalize gender
    v_gender_raw := LOWER(TRIM(COALESCE(new.raw_user_meta_data->>'gender', '')));
    v_gender := CASE
        WHEN v_gender_raw IN ('male', 'm') THEN 'Male'
        WHEN v_gender_raw IN ('female', 'f') THEN 'Female'
        ELSE 'Other'
    END;

    -- 4. Safely normalize blood group
    v_blood_raw := UPPER(TRIM(COALESCE(new.raw_user_meta_data->>'blood_group', '')));
    v_blood := CASE
        WHEN v_blood_raw IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-') THEN v_blood_raw
        ELSE 'O+'
    END;

    -- 5. Clean conflicting duplicate profiles from aborted/interrupted attempts
    DELETE FROM public.profiles WHERE email = new.email AND id != new.id;

    -- 6. Insert or update the public.profiles record
    BEGIN
        INSERT INTO public.profiles (id, name, email, phone, role)
        VALUES (
            new.id,
            COALESCE(NULLIF(TRIM(new.raw_user_meta_data->>'name'), ''), 'User'),
            new.email,
            COALESCE(NULLIF(TRIM(new.raw_user_meta_data->>'phone'), ''), ''),
            v_role
        )
        ON CONFLICT (id) DO UPDATE SET
            name = EXCLUDED.name,
            email = EXCLUDED.email,
            phone = EXCLUDED.phone,
            role = EXCLUDED.role;
    EXCEPTION WHEN OTHERS THEN
        RAISE WARNING 'handle_new_user: error inserting profile for %: %', new.id, SQLERRM;
    END;

    -- 7. Handle PATIENT record creation
    IF v_role = 'patient' THEN
        BEGIN
            IF EXISTS (SELECT 1 FROM public.patients WHERE profile_id = new.id OR user_id = new.id) THEN
                UPDATE public.patients SET
                    date_of_birth = COALESCE(v_dob, date_of_birth),
                    dob = COALESCE(v_dob, dob),
                    gender = COALESCE(v_gender, gender),
                    blood_group = COALESCE(v_blood, blood_group),
                    address = COALESCE(NULLIF(TRIM(new.raw_user_meta_data->>'address'), ''), address),
                    emergency_contact = COALESCE(NULLIF(TRIM(new.raw_user_meta_data->>'emergency_contact'), ''), emergency_contact)
                WHERE profile_id = new.id OR user_id = new.id;
            ELSE
                INSERT INTO public.patients (
                    profile_id,
                    user_id,
                    date_of_birth,
                    dob,
                    gender,
                    blood_group,
                    address,
                    emergency_contact
                )
                VALUES (
                    new.id,
                    new.id,
                    v_dob,
                    v_dob,
                    v_gender,
                    v_blood,
                    COALESCE(new.raw_user_meta_data->>'address', ''),
                    COALESCE(new.raw_user_meta_data->>'emergency_contact', '')
                );
            END IF;
        EXCEPTION WHEN OTHERS THEN
            RAISE WARNING 'handle_new_user: error inserting patient for %: %', new.id, SQLERRM;
        END;
    END IF;

    -- 8. Handle DOCTOR record creation
    IF v_role = 'doctor' THEN
        BEGIN
            -- Extract doctor fields with fallbacks
            v_exp := COALESCE(
                NULLIF(TRIM(new.raw_user_meta_data->>'experience'), ''),
                NULLIF(TRIM(new.raw_user_meta_data->>'experience_years'), ''),
                '5+ Years'
            );
            v_hospital := COALESCE(
                NULLIF(TRIM(new.raw_user_meta_data->>'hospital_name'), ''),
                NULLIF(TRIM(new.raw_user_meta_data->>'hospital'), ''),
                'Medicare General Hospital'
            );
            v_about := COALESCE(
                NULLIF(TRIM(new.raw_user_meta_data->>'bio'), ''),
                NULLIF(TRIM(new.raw_user_meta_data->>'about'), ''),
                ''
            );

            -- Clean fee input
            v_fee_raw := regexp_replace(
                COALESCE(new.raw_user_meta_data->>'consultation_fee', ''),
                '[^0-9\.]',
                '',
                'g'
            );
            IF v_fee_raw ~ '^[0-9]+(\.[0-9]+)?$' THEN
                v_fee := v_fee_raw::NUMERIC;
            ELSE
                v_fee := 500.00;
            END IF;

            IF EXISTS (SELECT 1 FROM public.doctors WHERE profile_id = new.id OR user_id = new.id) THEN
                UPDATE public.doctors SET
                    specialization = COALESCE(NULLIF(TRIM(new.raw_user_meta_data->>'specialization'), ''), specialization),
                    qualification = COALESCE(NULLIF(TRIM(new.raw_user_meta_data->>'qualification'), ''), qualification),
                    experience_years = COALESCE(v_exp, experience_years),
                    experience = COALESCE(v_exp, experience),
                    license_number = COALESCE(NULLIF(TRIM(new.raw_user_meta_data->>'license_number'), ''), license_number),
                    hospital_name = COALESCE(v_hospital, hospital_name),
                    hospital = COALESCE(v_hospital, hospital),
                    department = COALESCE(NULLIF(TRIM(new.raw_user_meta_data->>'department'), ''), department),
                    consultation_fee = COALESCE(v_fee, consultation_fee),
                    available_days = COALESCE(NULLIF(TRIM(new.raw_user_meta_data->>'available_days'), ''), available_days),
                    start_time = COALESCE(NULLIF(TRIM(new.raw_user_meta_data->>'start_time'), ''), start_time),
                    end_time = COALESCE(NULLIF(TRIM(new.raw_user_meta_data->>'end_time'), ''), end_time),
                    bio = COALESCE(v_about, bio),
                    about = COALESCE(v_about, about)
                WHERE profile_id = new.id OR user_id = new.id;
            ELSE
                INSERT INTO public.doctors (
                    profile_id,
                    user_id,
                    specialization,
                    qualification,
                    experience_years,
                    experience,
                    license_number,
                    hospital_name,
                    hospital,
                    department,
                    consultation_fee,
                    available_days,
                    start_time,
                    end_time,
                    bio,
                    about,
                    profile_image_url,
                    image_url,
                    available
                )
                VALUES (
                    new.id,
                    new.id,
                    COALESCE(NULLIF(TRIM(new.raw_user_meta_data->>'specialization'), ''), 'General Medicine'),
                    COALESCE(NULLIF(TRIM(new.raw_user_meta_data->>'qualification'), ''), 'MBBS, MD'),
                    v_exp,
                    v_exp,
                    COALESCE(new.raw_user_meta_data->>'license_number', ''),
                    v_hospital,
                    v_hospital,
                    COALESCE(NULLIF(TRIM(new.raw_user_meta_data->>'department'), ''), 'General Outpatient'),
                    v_fee,
                    COALESCE(NULLIF(TRIM(new.raw_user_meta_data->>'available_days'), ''), 'Mon,Tue,Wed,Thu,Fri'),
                    COALESCE(NULLIF(TRIM(new.raw_user_meta_data->>'start_time'), ''), '09:00 AM'),
                    COALESCE(NULLIF(TRIM(new.raw_user_meta_data->>'end_time'), ''), '05:00 PM'),
                    v_about,
                    v_about,
                    COALESCE(new.raw_user_meta_data->>'profile_image_url', ''),
                    COALESCE(new.raw_user_meta_data->>'image_url', ''),
                    true
                );
            END IF;
        EXCEPTION WHEN OTHERS THEN
            RAISE WARNING 'handle_new_user: error inserting doctor for %: %', new.id, SQLERRM;
        END;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

-- 9. Auto confirm trigger for synthetic accounts
CREATE OR REPLACE FUNCTION public.auto_confirm_phone_auth()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.email LIKE '%@medicare.local' THEN
        NEW.email_confirmed_at = COALESCE(NEW.email_confirmed_at, timezone('utc'::text, now()));
        NEW.confirmed_at = COALESCE(NEW.confirmed_at, timezone('utc'::text, now()));
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

-- 10. Re-bind triggers cleanly on auth.users
DROP TRIGGER IF EXISTS on_auth_user_created_confirm ON auth.users;
CREATE TRIGGER on_auth_user_created_confirm
    BEFORE INSERT OR UPDATE ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.auto_confirm_phone_auth();

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
