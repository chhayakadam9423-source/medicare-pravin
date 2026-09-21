export type UserRole = 'patient' | 'doctor' | 'admin';

export interface UserProfile {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  phone?: string;
  avatar_url?: string;
}

export interface DoctorItem {
  id: string;
  user_id: string;
  specialization: string;
  qualification: string;
  experience: string;
  about: string;
  available: boolean;
  rating?: number;
  profile?: UserProfile;
}

export interface PatientItem {
  id: string;
  user_id: string;
  dob?: string;
  gender?: string;
  blood_group?: string;
  medical_history?: string;
  profile?: UserProfile;
}

export interface AppointmentItem {
  id: string;
  patient_id: string;
  doctor_id: string;
  appointment_date: string;
  appointment_time: string;
  reason: string;
  status: 'pending' | 'confirmed' | 'rejected' | 'completed' | 'cancelled';
  doctor?: DoctorItem;
  patient?: PatientItem;
  created_at?: string;
}

export interface PrescriptionItem {
  id: string;
  appointment_id: string;
  medicine: string;
  dosage: string;
  frequency: string;
  instructions: string;
  created_at: string;
  doctor_name?: string;
  doctor_specialty?: string;
}

export type AndroidScreen = 
  | 'splash'
  | 'welcome'
  | 'login'
  | 'register'
  // Patient screens
  | 'patient_dashboard'
  | 'doctor_list'
  | 'doctor_details'
  | 'book_appointment'
  | 'my_appointments'
  | 'prescription_details'
  | 'patient_profile'
  // Doctor screens
  | 'doctor_dashboard'
  | 'doctor_appointments'
  | 'add_prescription'
  | 'doctor_profile'
  // Admin screens
  | 'admin_dashboard'
  | 'admin_manage_doctors'
  | 'admin_add_doctor'
  | 'admin_manage_patients';
