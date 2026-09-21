import { DoctorItem, PatientItem, AppointmentItem, PrescriptionItem, UserProfile } from './types';

export const initialProfiles: UserProfile[] = [
  {
    id: 'user-pat-1',
    email: 'patient@medicare.com',
    name: 'Alex Johnson',
    role: 'patient',
    phone: '+1-555-0201'
  },
  {
    id: 'user-doc-1',
    email: 'doctor@medicare.com',
    name: 'Dr. Sarah Jenkins',
    role: 'doctor',
    phone: '+1-555-0101'
  },
  {
    id: 'user-doc-2',
    email: 'm.chen@medicare.com',
    name: 'Dr. Michael Chen',
    role: 'doctor',
    phone: '+1-555-0102'
  },
  {
    id: 'user-doc-3',
    email: 'a.patel@medicare.com',
    name: 'Dr. Anita Patel',
    role: 'doctor',
    phone: '+1-555-0103'
  },
  {
    id: 'user-doc-4',
    email: 'r.stone@medicare.com',
    name: 'Dr. Robert Stone',
    role: 'doctor',
    phone: '+1-555-0104'
  },
  {
    id: 'user-admin-1',
    email: 'admin@medicare.com',
    name: 'Hospital Administrator',
    role: 'admin',
    phone: '+1-555-0900'
  }
];

export const initialDoctors: DoctorItem[] = [
  {
    id: 'doc-1',
    user_id: 'user-doc-1',
    specialization: 'Cardiologist',
    qualification: 'MBBS, MD (Cardiology), FACC',
    experience: '12 Years',
    about: 'Senior Consultant Cardiologist dedicated to comprehensive cardiovascular healthcare, preventive cardiology, hypertension control, and patient-centered rehabilitation.',
    available: true,
    rating: 4.9,
    profile: initialProfiles[1]
  },
  {
    id: 'doc-2',
    user_id: 'user-doc-2',
    specialization: 'Neurologist',
    qualification: 'MD, DM (Neurology)',
    experience: '9 Years',
    about: 'Specialized in neurological diagnostic evaluation, migraine therapy, neuro-rehabilitation, and treatment of complex neuro-muscular disorders.',
    available: true,
    rating: 4.8,
    profile: initialProfiles[2]
  },
  {
    id: 'doc-3',
    user_id: 'user-doc-3',
    specialization: 'Pediatrician',
    qualification: 'MBBS, DCH, FAAP',
    experience: '14 Years',
    about: 'Passionate child healthcare specialist focusing on neonatal care, infant nutrition, developmental pediatrics, and preventive immunizations.',
    available: true,
    rating: 5.0,
    profile: initialProfiles[3]
  },
  {
    id: 'doc-4',
    user_id: 'user-doc-4',
    specialization: 'Orthopedic',
    qualification: 'MS (Orthopedics), MCh',
    experience: '11 Years',
    about: 'Orthopedic surgeon focusing on joint restoration, sports injury rehabilitation, arthroscopic interventions, and trauma surgery.',
    available: false,
    rating: 4.7,
    profile: initialProfiles[4]
  }
];

export const initialPatients: PatientItem[] = [
  {
    id: 'pat-1',
    user_id: 'user-pat-1',
    dob: '1995-04-12',
    gender: 'Male',
    blood_group: 'O+',
    medical_history: 'Mild seasonal allergies, no prior major surgeries.',
    profile: initialProfiles[0]
  },
  {
    id: 'pat-2',
    user_id: 'user-pat-2',
    dob: '1988-11-23',
    gender: 'Female',
    blood_group: 'A+',
    medical_history: 'Controlled asthma, routine health checkup ongoing.',
    profile: {
      id: 'user-pat-2',
      email: 'emma.watson@gmail.com',
      name: 'Emma Watson',
      role: 'patient',
      phone: '+1-555-0322'
    }
  },
  {
    id: 'pat-3',
    user_id: 'user-pat-3',
    dob: '1992-07-19',
    gender: 'Male',
    blood_group: 'B+',
    medical_history: 'Routine hypertension follow-up.',
    profile: {
      id: 'user-pat-3',
      email: 'david.miller@gmail.com',
      name: 'David Miller',
      role: 'patient',
      phone: '+1-555-0481'
    }
  }
];

export const initialAppointments: AppointmentItem[] = [
  {
    id: 'apt-101',
    patient_id: 'pat-1',
    doctor_id: 'doc-1',
    appointment_date: '2026-09-22',
    appointment_time: '10:30 AM',
    reason: 'Routine cardiac wellness checkup and blood pressure monitoring.',
    status: 'pending',
    doctor: initialDoctors[0],
    patient: initialPatients[0],
    created_at: '2026-09-21T08:30:00Z'
  },
  {
    id: 'apt-102',
    patient_id: 'pat-1',
    doctor_id: 'doc-2',
    appointment_date: '2026-09-15',
    appointment_time: '02:00 PM',
    reason: 'Recurring tension headache and eye strain consultation.',
    status: 'completed',
    doctor: initialDoctors[1],
    patient: initialPatients[0],
    created_at: '2026-09-14T11:00:00Z'
  },
  {
    id: 'apt-103',
    patient_id: 'pat-2',
    doctor_id: 'doc-1',
    appointment_date: '2026-09-23',
    appointment_time: '09:00 AM',
    reason: 'Palpitation evaluation post workout.',
    status: 'confirmed',
    doctor: initialDoctors[0],
    patient: initialPatients[1],
    created_at: '2026-09-20T14:15:00Z'
  }
];

export const initialPrescriptions: PrescriptionItem[] = [
  {
    id: 'rx-201',
    appointment_id: 'apt-102',
    medicine: 'Naproxen Sodium 250mg & Magnesium Glycinate 200mg',
    dosage: '1 Tablet',
    frequency: 'Twice daily after meals (1-0-1)',
    instructions: 'Take with plenty of water. Avoid staring at bright computer screens for extended periods. Return for review in 2 weeks.',
    created_at: '2026-09-15T14:45:00Z',
    doctor_name: 'Dr. Michael Chen',
    doctor_specialty: 'Neurologist'
  }
];
