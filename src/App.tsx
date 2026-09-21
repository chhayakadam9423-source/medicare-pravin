import React, { useState } from 'react';
import { 
  Heart, Calendar, Clock, User, Shield, Stethoscope, ChevronRight,
  Phone, AlertCircle, CheckCircle2, XCircle, Search, Plus, FileText,
  LogOut, ArrowLeft, Database, Code, Smartphone, Pill
} from 'lucide-react';
import { 
  initialProfiles, initialDoctors, initialPatients, 
  initialAppointments, initialPrescriptions 
} from './initialData';
import { 
  AndroidScreen, UserProfile, DoctorItem, 
  PatientItem, AppointmentItem, PrescriptionItem 
} from './types';

export default function App() {
  // Database state (mimicking Supabase PostgreSQL tables in real-time)
  const [profiles, setProfiles] = useState<UserProfile[]>(initialProfiles);
  const [doctors, setDoctors] = useState<DoctorItem[]>(initialDoctors);
  const [patients, setPatients] = useState<PatientItem[]>(initialPatients);
  const [appointments, setAppointments] = useState<AppointmentItem[]>(initialAppointments);
  const [prescriptions, setPrescriptions] = useState<PrescriptionItem[]>(initialPrescriptions);

  // Active Session & Screen
  const [currentUser, setCurrentUser] = useState<UserProfile>(initialProfiles[0]);
  const [currentScreen, setCurrentScreen] = useState<AndroidScreen>('patient_dashboard');
  const [selectedDoctor, setSelectedDoctor] = useState<DoctorItem>(initialDoctors[0]);
  const [selectedAppointment, setSelectedAppointment] = useState<AppointmentItem | null>(null);
  const [selectedPrescription, setSelectedPrescription] = useState<PrescriptionItem | null>(initialPrescriptions[0]);
  
  // Filters & Inputs
  const [doctorSearch, setDoctorSearch] = useState('');
  const [specialtyFilter, setSpecialtyFilter] = useState('All');
  const [bookingDate, setBookingDate] = useState('2026-09-22');
  const [bookingTime, setBookingTime] = useState('10:30 AM');
  const [bookingReason, setBookingReason] = useState('');
  
  // Add Prescription state
  const [rxMedicine, setRxMedicine] = useState('');
  const [rxDosage, setRxDosage] = useState('');
  const [rxFrequency, setRxFrequency] = useState('');
  const [rxInstructions, setRxInstructions] = useState('');

  // Add Doctor state (Admin)
  const [newDocName, setNewDocName] = useState('');
  const [newDocEmail, setNewDocEmail] = useState('');
  const [newDocPhone, setNewDocPhone] = useState('');
  const [newDocSpecialty, setNewDocSpecialty] = useState('');
  const [newDocQual, setNewDocQual] = useState('MBBS, MD');
  const [newDocExp, setNewDocExp] = useState('8 Years');
  const [newDocAbout, setNewDocAbout] = useState('');

  // UI Tabs (Phone Simulator vs Supabase Database vs Android Code Viewer)
  const [activeTab, setActiveTab] = useState<'simulator' | 'database' | 'code'>('simulator');
  const [dbTable, setDbTable] = useState<'appointments' | 'doctors' | 'patients' | 'prescriptions' | 'profiles'>('appointments');
  const [codeFile, setCodeFile] = useState<string>('schema.sql');
  const [notification, setNotification] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setNotification(msg);
    setTimeout(() => setNotification(null), 3500);
  };

  // Switch Role Helper
  const switchUser = (role: 'patient' | 'doctor' | 'admin') => {
    if (role === 'patient') {
      setCurrentUser(profiles[0]);
      setCurrentScreen('patient_dashboard');
    } else if (role === 'doctor') {
      setCurrentUser(profiles[1]);
      setCurrentScreen('doctor_dashboard');
    } else {
      setCurrentUser(profiles[5]);
      setCurrentScreen('admin_dashboard');
    }
    showToast(`Switched active session to ${role.toUpperCase()}`);
  };

  // Actions
  const handleBookAppointment = () => {
    if (!bookingReason.trim()) {
      showToast('Please enter reason for consultation');
      return;
    }

    const currentPatient = patients.find(p => p.user_id === currentUser.id) || patients[0];
    const newApt: AppointmentItem = {
      id: `apt-${Date.now().toString().slice(-4)}`,
      patient_id: currentPatient.id,
      doctor_id: selectedDoctor.id,
      appointment_date: bookingDate,
      appointment_time: bookingTime,
      reason: bookingReason,
      status: 'pending',
      doctor: selectedDoctor,
      patient: currentPatient,
      created_at: new Date().toISOString()
    };

    setAppointments([newApt, ...appointments]);
    setBookingReason('');
    showToast('Appointment booked! Reflected in Supabase database.');
    setCurrentScreen('my_appointments');
  };

  const handleUpdateAppointmentStatus = (id: string, newStatus: AppointmentItem['status']) => {
    setAppointments(prev => prev.map(a => a.id === id ? { ...a, status: newStatus } : a));
    showToast(`Appointment status updated to ${newStatus.toUpperCase()}`);
  };

  const handleSavePrescription = () => {
    if (!rxMedicine.trim() || !rxDosage.trim()) {
      showToast('Medicine and dosage are required');
      return;
    }

    const newRx: PrescriptionItem = {
      id: `rx-${Date.now().toString().slice(-4)}`,
      appointment_id: selectedAppointment?.id || 'apt-101',
      medicine: rxMedicine,
      dosage: rxDosage,
      frequency: rxFrequency || 'Twice daily after meals',
      instructions: rxInstructions || 'Take with water as directed.',
      created_at: new Date().toISOString(),
      doctor_name: currentUser.name,
      doctor_specialty: 'Cardiologist'
    };

    setPrescriptions([newRx, ...prescriptions]);
    setRxMedicine('');
    setRxDosage('');
    setRxFrequency('');
    setRxInstructions('');
    showToast('Prescription saved to Supabase digital health records.');
    setCurrentScreen('doctor_dashboard');
  };

  const handleAdminAddDoctor = () => {
    if (!newDocName.trim() || !newDocSpecialty.trim()) {
      showToast('Name and Specialization are required');
      return;
    }

    const newUserId = `user-doc-${Date.now().toString().slice(-4)}`;
    const newProf: UserProfile = {
      id: newUserId,
      email: newDocEmail || `${newDocName.toLowerCase().replace(/[^a-z]/g, '')}@medicare.com`,
      name: newDocName.startsWith('Dr.') ? newDocName : `Dr. ${newDocName}`,
      role: 'doctor',
      phone: newDocPhone || '+1-555-0199'
    };

    const newDoc: DoctorItem = {
      id: `doc-${Date.now().toString().slice(-4)}`,
      user_id: newUserId,
      specialization: newDocSpecialty,
      qualification: newDocQual,
      experience: newDocExp,
      about: newDocAbout || 'Clinical practitioner dedicated to patient healthcare excellence.',
      available: true,
      rating: 5.0,
      profile: newProf
    };

    setProfiles([...profiles, newProf]);
    setDoctors([...doctors, newDoc]);
    setNewDocName('');
    setNewDocEmail('');
    setNewDocPhone('');
    setNewDocSpecialty('');
    showToast('Doctor enrolled in Medicare and synced to Supabase.');
    setCurrentScreen('admin_manage_doctors');
  };

  const handleDeleteDoctor = (id: string) => {
    setDoctors(prev => prev.filter(d => d.id !== id));
    showToast('Doctor removed from hospital directory.');
  };

  // Filtered Doctors
  const filteredDoctors = doctors.filter(doc => {
    const matchesSearch = (doc.profile?.name || '').toLowerCase().includes(doctorSearch.toLowerCase()) ||
                          doc.specialization.toLowerCase().includes(doctorSearch.toLowerCase());
    const matchesSpecialty = specialtyFilter === 'All' || doc.specialization.toLowerCase() === specialtyFilter.toLowerCase();
    return matchesSearch && matchesSpecialty;
  });

  // Current Patient's Appointments
  const userAppointments = appointments.filter(a => {
    if (currentUser.role === 'patient') {
      const p = patients.find(pat => pat.user_id === currentUser.id);
      return a.patient_id === (p?.id || 'pat-1');
    }
    return true;
  });

  return (
    <div id="medicare_app_root" className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      {/* Top Navigation Bar */}
      <header className="bg-slate-900/90 border-b border-slate-800 px-6 py-3.5 flex flex-wrap items-center justify-between gap-4 sticky top-0 z-50 backdrop-blur">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-cyan-600 to-teal-400 flex items-center justify-center shadow-lg shadow-cyan-900/30">
            <Heart className="w-5 h-5 text-white fill-white" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-lg text-white tracking-tight">MEDICARE</span>
              <span className="text-[11px] px-2 py-0.5 rounded-full font-semibold bg-cyan-950 text-cyan-400 border border-cyan-800/60">
                Android Kotlin + Supabase
              </span>
            </div>
            <p className="text-xs text-slate-400">Hospital Management System • Diploma Project</p>
          </div>
        </div>

        {/* View Switcher Tabs */}
        <div className="flex items-center bg-slate-950 p-1 rounded-xl border border-slate-800">
          <button
            onClick={() => setActiveTab('simulator')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-semibold transition ${
              activeTab === 'simulator' ? 'bg-cyan-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Smartphone className="w-4 h-4" />
            Android Device Simulator
          </button>
          <button
            onClick={() => setActiveTab('database')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-semibold transition ${
              activeTab === 'database' ? 'bg-cyan-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Database className="w-4 h-4" />
            Supabase Live DB ({appointments.length} Apts)
          </button>
          <button
            onClick={() => setActiveTab('code')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-semibold transition ${
              activeTab === 'code' ? 'bg-cyan-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Code className="w-4 h-4" />
            Kotlin &amp; XML Source
          </button>
        </div>

        {/* Quick Role Switcher */}
        <div className="flex items-center gap-2 text-xs">
          <span className="text-slate-400 hidden sm:inline">Active Role:</span>
          <button
            onClick={() => switchUser('patient')}
            className={`px-3 py-1.5 rounded-lg font-medium transition ${
              currentUser.role === 'patient' 
                ? 'bg-emerald-600 text-white font-bold ring-2 ring-emerald-400/30' 
                : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
            }`}
          >
            👤 Patient
          </button>
          <button
            onClick={() => switchUser('doctor')}
            className={`px-3 py-1.5 rounded-lg font-medium transition ${
              currentUser.role === 'doctor' 
                ? 'bg-cyan-600 text-white font-bold ring-2 ring-cyan-400/30' 
                : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
            }`}
          >
            🩺 Doctor
          </button>
          <button
            onClick={() => switchUser('admin')}
            className={`px-3 py-1.5 rounded-lg font-medium transition ${
              currentUser.role === 'admin' 
                ? 'bg-purple-600 text-white font-bold ring-2 ring-purple-400/30' 
                : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
            }`}
          >
            🛡️ Admin
          </button>
        </div>
      </header>

      {/* Floating Notification */}
      {notification && (
        <div className="fixed bottom-6 right-6 z-50 bg-slate-900 border border-cyan-500/40 text-cyan-200 px-4 py-3 rounded-xl shadow-2xl flex items-center gap-3 text-sm animate-bounce">
          <CheckCircle2 className="w-5 h-5 text-cyan-400 shrink-0" />
          <span>{notification}</span>
        </div>
      )}

      {/* Main Content Area */}
      <main className="flex-1 p-4 md:p-8 flex items-center justify-center overflow-auto">
        {activeTab === 'simulator' && (
          <div className="w-full max-w-sm mx-auto">
            {/* Phone Container */}
            <div className="relative mx-auto border-[10px] border-slate-900 rounded-[44px] shadow-2xl bg-white text-slate-900 overflow-hidden w-[380px] h-[780px] flex flex-col ring-1 ring-slate-800">
              
              {/* Android Status Bar */}
              <div className="bg-slate-900 text-white px-6 pt-2 pb-1.5 flex justify-between items-center text-xs select-none">
                <span className="font-semibold text-[13px]">09:41</span>
                {/* Camera punch hole */}
                <div className="w-4 h-4 rounded-full bg-black mx-auto" />
                <div className="flex items-center space-x-1.5 text-[11px]">
                  <span>5G</span>
                  <span>📶</span>
                  <span>100%</span>
                </div>
              </div>

              {/* Screen Body */}
              <div className="flex-1 overflow-y-auto bg-slate-50 flex flex-col">
                
                {/* 1. SPLASH SCREEN */}
                {currentScreen === 'splash' && (
                  <div className="flex-1 flex flex-col items-center justify-center p-8 bg-gradient-to-br from-cyan-600 to-teal-700 text-white text-center">
                    <div className="w-24 h-24 rounded-3xl bg-white/20 backdrop-blur p-4 mb-6 shadow-2xl flex items-center justify-center">
                      <Heart className="w-14 h-14 text-white fill-white animate-pulse" />
                    </div>
                    <h1 className="text-3xl font-extrabold tracking-tight mb-2">MEDICARE</h1>
                    <p className="text-cyan-100 text-sm mb-8">Modern Hospital Management</p>
                    <button
                      onClick={() => setCurrentScreen('welcome')}
                      className="w-full bg-white text-cyan-700 font-bold py-3.5 rounded-xl shadow-lg hover:bg-cyan-50 transition"
                    >
                      ENTER APP
                    </button>
                  </div>
                )}

                {/* 2. WELCOME SCREEN */}
                {currentScreen === 'welcome' && (
                  <div className="flex-1 flex flex-col justify-between p-6 bg-white">
                    <div className="pt-6">
                      <div className="w-14 h-14 rounded-2xl bg-cyan-100 text-cyan-600 flex items-center justify-center mb-5">
                        <Heart className="w-8 h-8 fill-cyan-600" />
                      </div>
                      <h2 className="text-2xl font-bold text-slate-900 leading-snug">
                        Quality Healthcare at Your Fingertips
                      </h2>
                      <p className="text-slate-500 text-sm mt-2">
                        Connect with expert specialists, schedule appointments seamlessly, and access digital prescriptions securely.
                      </p>

                      <div className="mt-8 space-y-3">
                        <div className="flex items-center gap-3 p-3 rounded-xl bg-slate-50 border border-slate-100">
                          <Stethoscope className="w-5 h-5 text-cyan-600" />
                          <span className="text-xs font-semibold text-slate-700">Verified Board-Certified Doctors</span>
                        </div>
                        <div className="flex items-center gap-3 p-3 rounded-xl bg-slate-50 border border-slate-100">
                          <Calendar className="w-5 h-5 text-emerald-600" />
                          <span className="text-xs font-semibold text-slate-700">Real-Time Instant Appointment Booking</span>
                        </div>
                        <div className="flex items-center gap-3 p-3 rounded-xl bg-slate-50 border border-slate-100">
                          <Pill className="w-5 h-5 text-blue-600" />
                          <span className="text-xs font-semibold text-slate-700">Supabase Secured Digital Prescriptions</span>
                        </div>
                      </div>
                    </div>

                    <div className="space-y-3 pb-2">
                      <button
                        onClick={() => setCurrentScreen('login')}
                        className="w-full bg-cyan-600 hover:bg-cyan-700 text-white font-bold py-3.5 rounded-xl shadow-md transition"
                      >
                        SIGN IN
                      </button>
                      <button
                        onClick={() => setCurrentScreen('register')}
                        className="w-full bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold py-3.5 rounded-xl transition"
                      >
                        CREATE ACCOUNT
                      </button>
                    </div>
                  </div>
                )}

                {/* 3. LOGIN SCREEN */}
                {currentScreen === 'login' && (
                  <div className="flex-1 flex flex-col justify-between p-6 bg-white">
                    <div>
                      <button onClick={() => setCurrentScreen('welcome')} className="p-2 -ml-2 text-slate-500">
                        <ArrowLeft className="w-5 h-5" />
                      </button>
                      <h2 className="text-2xl font-bold text-slate-900 mt-3">Welcome Back</h2>
                      <p className="text-slate-500 text-xs mt-1">Sign in with your Medicare credentials</p>

                      <div className="mt-6 space-y-4">
                        <div>
                          <label className="text-xs font-semibold text-slate-700 block mb-1">Email Address</label>
                          <input 
                            type="email" 
                            defaultValue={currentUser.email}
                            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:border-cyan-600 bg-slate-50"
                          />
                        </div>
                        <div>
                          <label className="text-xs font-semibold text-slate-700 block mb-1">Password</label>
                          <input 
                            type="password" 
                            defaultValue="Password123!" 
                            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:border-cyan-600 bg-slate-50"
                          />
                        </div>
                      </div>

                      {/* Demo Quick Logins */}
                      <div className="mt-6 p-3 rounded-xl bg-cyan-50 border border-cyan-100">
                        <p className="text-[11px] font-bold text-cyan-800 uppercase tracking-wider mb-2">Quick 1-Click Demo Login:</p>
                        <div className="flex gap-1.5 flex-wrap">
                          <button
                            onClick={() => { switchUser('patient'); setCurrentScreen('patient_dashboard'); }}
                            className="px-2.5 py-1 text-xs font-bold rounded-lg bg-emerald-600 text-white"
                          >
                            Patient
                          </button>
                          <button
                            onClick={() => { switchUser('doctor'); setCurrentScreen('doctor_dashboard'); }}
                            className="px-2.5 py-1 text-xs font-bold rounded-lg bg-cyan-600 text-white"
                          >
                            Doctor
                          </button>
                          <button
                            onClick={() => { switchUser('admin'); setCurrentScreen('admin_dashboard'); }}
                            className="px-2.5 py-1 text-xs font-bold rounded-lg bg-purple-600 text-white"
                          >
                            Admin
                          </button>
                        </div>
                      </div>
                    </div>

                    <div className="space-y-3 pb-2">
                      <button
                        onClick={() => {
                          if (currentUser.role === 'patient') setCurrentScreen('patient_dashboard');
                          else if (currentUser.role === 'doctor') setCurrentScreen('doctor_dashboard');
                          else setCurrentScreen('admin_dashboard');
                        }}
                        className="w-full bg-cyan-600 hover:bg-cyan-700 text-white font-bold py-3.5 rounded-xl shadow-md transition"
                      >
                        SIGN IN
                      </button>
                      <p className="text-center text-xs text-slate-500">
                        Don't have an account?{' '}
                        <button onClick={() => setCurrentScreen('register')} className="text-cyan-600 font-bold">
                          Register
                        </button>
                      </p>
                    </div>
                  </div>
                )}

                {/* 4. REGISTER SCREEN */}
                {currentScreen === 'register' && (
                  <div className="flex-1 flex flex-col justify-between p-6 bg-white overflow-y-auto">
                    <div>
                      <button onClick={() => setCurrentScreen('welcome')} className="p-2 -ml-2 text-slate-500">
                        <ArrowLeft className="w-5 h-5" />
                      </button>
                      <h2 className="text-2xl font-bold text-slate-900 mt-2">Create Account</h2>
                      <p className="text-slate-500 text-xs mt-1">Join the Medicare Healthcare Network</p>

                      <div className="mt-4 space-y-3">
                        <div>
                          <label className="text-xs font-semibold text-slate-700 block mb-1">Full Name</label>
                          <input 
                            placeholder="e.g. John Doe"
                            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:border-cyan-600 bg-slate-50"
                          />
                        </div>
                        <div>
                          <label className="text-xs font-semibold text-slate-700 block mb-1">Email</label>
                          <input 
                            placeholder="john@example.com"
                            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:border-cyan-600 bg-slate-50"
                          />
                        </div>
                        <div>
                          <label className="text-xs font-semibold text-slate-700 block mb-1">Phone</label>
                          <input 
                            placeholder="+1-555-0100"
                            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:border-cyan-600 bg-slate-50"
                          />
                        </div>
                        <div>
                          <label className="text-xs font-semibold text-slate-700 block mb-1">Password</label>
                          <input 
                            type="password"
                            defaultValue="Password123!"
                            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:border-cyan-600 bg-slate-50"
                          />
                        </div>
                      </div>
                    </div>

                    <div className="mt-6 space-y-3 pb-2">
                      <button
                        onClick={() => {
                          showToast('Registration successful! Directing to Dashboard.');
                          setCurrentScreen('patient_dashboard');
                        }}
                        className="w-full bg-cyan-600 hover:bg-cyan-700 text-white font-bold py-3.5 rounded-xl shadow-md transition"
                      >
                        CREATE ACCOUNT
                      </button>
                      <p className="text-center text-xs text-slate-500">
                        Already have an account?{' '}
                        <button onClick={() => setCurrentScreen('login')} className="text-cyan-600 font-bold">
                          Sign In
                        </button>
                      </p>
                    </div>
                  </div>
                )}

                {/* 5. PATIENT DASHBOARD */}
                {currentScreen === 'patient_dashboard' && (
                  <div className="flex-1 flex flex-col justify-between">
                    <div className="p-4 space-y-4">
                      {/* Greeting Header */}
                      <div className="flex items-center justify-between">
                        <div>
                          <h3 className="text-lg font-bold text-slate-900">Hello, {currentUser.name}</h3>
                          <p className="text-xs text-slate-500">How are you feeling today?</p>
                        </div>
                        <button 
                          onClick={() => setCurrentScreen('patient_profile')}
                          className="w-10 h-10 rounded-full bg-cyan-100 text-cyan-700 font-bold flex items-center justify-center border border-cyan-200"
                        >
                          {currentUser.name.slice(0, 2).toUpperCase()}
                        </button>
                      </div>

                      {/* Quick Search Banner */}
                      <div 
                        onClick={() => setCurrentScreen('doctor_list')}
                        className="bg-white p-3 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-3 cursor-pointer text-slate-400 text-xs"
                      >
                        <Search className="w-4 h-4 text-cyan-600" />
                        <span>Search doctors, specialists...</span>
                      </div>

                      {/* 4 Action Grid */}
                      <div className="grid grid-cols-4 gap-2">
                        <button 
                          onClick={() => setCurrentScreen('doctor_list')}
                          className="bg-cyan-50 hover:bg-cyan-100 border border-cyan-100 p-2.5 rounded-2xl flex flex-col items-center gap-1.5 transition"
                        >
                          <div className="w-8 h-8 rounded-xl bg-cyan-600 text-white flex items-center justify-center">
                            <Stethoscope className="w-4 h-4" />
                          </div>
                          <span className="text-[10px] font-bold text-slate-700">Find Doc</span>
                        </button>
                        <button 
                          onClick={() => setCurrentScreen('my_appointments')}
                          className="bg-emerald-50 hover:bg-emerald-100 border border-emerald-100 p-2.5 rounded-2xl flex flex-col items-center gap-1.5 transition"
                        >
                          <div className="w-8 h-8 rounded-xl bg-emerald-600 text-white flex items-center justify-center">
                            <Calendar className="w-4 h-4" />
                          </div>
                          <span className="text-[10px] font-bold text-slate-700">Schedule</span>
                        </button>
                        <button 
                          onClick={() => setCurrentScreen('my_appointments')}
                          className="bg-blue-50 hover:bg-blue-100 border border-blue-100 p-2.5 rounded-2xl flex flex-col items-center gap-1.5 transition"
                        >
                          <div className="w-8 h-8 rounded-xl bg-blue-600 text-white flex items-center justify-center">
                            <Pill className="w-4 h-4" />
                          </div>
                          <span className="text-[10px] font-bold text-slate-700">Rx Notes</span>
                        </button>
                        <button 
                          onClick={() => showToast('Calling Medicare 24/7 Hotline: +1-800-MEDICARE')}
                          className="bg-rose-50 hover:bg-rose-100 border border-rose-100 p-2.5 rounded-2xl flex flex-col items-center gap-1.5 transition"
                        >
                          <div className="w-8 h-8 rounded-xl bg-rose-600 text-white flex items-center justify-center">
                            <Phone className="w-4 h-4" />
                          </div>
                          <span className="text-[10px] font-bold text-slate-700">911 SOS</span>
                        </button>
                      </div>

                      {/* Specialties Row */}
                      <div>
                        <div className="flex justify-between items-center mb-2">
                          <span className="text-xs font-bold text-slate-900">Specialties</span>
                          <button onClick={() => setCurrentScreen('doctor_list')} className="text-[11px] font-semibold text-cyan-600">
                            See all
                          </button>
                        </div>
                        <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-none">
                          {['Cardiologist', 'Neurologist', 'Pediatrician', 'Orthopedic'].map((s) => (
                            <button
                              key={s}
                              onClick={() => { setSpecialtyFilter(s); setCurrentScreen('doctor_list'); }}
                              className="px-3 py-1.5 bg-white border border-slate-200 rounded-xl text-xs font-semibold whitespace-nowrap text-slate-700 hover:border-cyan-600"
                            >
                              {s}
                            </button>
                          ))}
                        </div>
                      </div>

                      {/* Top Doctors */}
                      <div>
                        <div className="flex justify-between items-center mb-2">
                          <span className="text-xs font-bold text-slate-900">Top Physicians</span>
                          <button onClick={() => setCurrentScreen('doctor_list')} className="text-[11px] font-semibold text-cyan-600">
                            View All
                          </button>
                        </div>
                        <div className="space-y-2.5">
                          {doctors.slice(0, 2).map((doc) => (
                            <div 
                              key={doc.id}
                              className="bg-white p-3 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between"
                            >
                              <div className="flex items-center gap-3">
                                <div className="w-11 h-11 rounded-xl bg-cyan-100 text-cyan-700 font-bold flex items-center justify-center">
                                  {doc.profile?.name.slice(3, 5).toUpperCase() || 'DR'}
                                </div>
                                <div>
                                  <h4 className="text-xs font-bold text-slate-900">{doc.profile?.name}</h4>
                                  <p className="text-[11px] font-semibold text-cyan-600">{doc.specialization}</p>
                                  <p className="text-[10px] text-slate-400">⭐ {doc.rating} • {doc.experience}</p>
                                </div>
                              </div>
                              <button
                                onClick={() => {
                                  setSelectedDoctor(doc);
                                  setCurrentScreen('book_appointment');
                                }}
                                className="px-3 py-1.5 bg-cyan-600 text-white rounded-lg text-xs font-bold hover:bg-cyan-700"
                              >
                                Book
                              </button>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>

                    {/* Bottom Nav Bar */}
                    <div className="bg-white border-t border-slate-200 px-6 py-2.5 flex justify-between items-center">
                      <button onClick={() => setCurrentScreen('patient_dashboard')} className="flex flex-col items-center text-cyan-600">
                        <Heart className="w-5 h-5 fill-cyan-600" />
                        <span className="text-[10px] font-bold mt-0.5">Home</span>
                      </button>
                      <button onClick={() => setCurrentScreen('doctor_list')} className="flex flex-col items-center text-slate-400 hover:text-slate-600">
                        <Stethoscope className="w-5 h-5" />
                        <span className="text-[10px] font-medium mt-0.5">Doctors</span>
                      </button>
                      <button onClick={() => setCurrentScreen('my_appointments')} className="flex flex-col items-center text-slate-400 hover:text-slate-600">
                        <Calendar className="w-5 h-5" />
                        <span className="text-[10px] font-medium mt-0.5">Visits</span>
                      </button>
                      <button onClick={() => setCurrentScreen('patient_profile')} className="flex flex-col items-center text-slate-400 hover:text-slate-600">
                        <User className="w-5 h-5" />
                        <span className="text-[10px] font-medium mt-0.5">Profile</span>
                      </button>
                    </div>
                  </div>
                )}

                {/* 6. DOCTOR LIST SCREEN */}
                {currentScreen === 'doctor_list' && (
                  <div className="flex-1 flex flex-col justify-between">
                    <div className="p-4 space-y-3 overflow-y-auto">
                      <div className="flex items-center gap-2">
                        <button onClick={() => setCurrentScreen('patient_dashboard')} className="p-1 -ml-1 text-slate-500">
                          <ArrowLeft className="w-5 h-5" />
                        </button>
                        <h3 className="text-base font-bold text-slate-900">Find Doctors</h3>
                      </div>

                      {/* Search & Filter */}
                      <input 
                        type="text" 
                        value={doctorSearch}
                        onChange={(e) => setDoctorSearch(e.target.value)}
                        placeholder="Search doctor or specialty..."
                        className="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none focus:border-cyan-600 bg-white"
                      />

                      {/* Specialty Filter Chips */}
                      <div className="flex gap-1.5 overflow-x-auto pb-1 scrollbar-none">
                        {['All', 'Cardiologist', 'Neurologist', 'Pediatrician', 'Orthopedic'].map((s) => (
                          <button
                            key={s}
                            onClick={() => setSpecialtyFilter(s)}
                            className={`px-3 py-1 rounded-full text-[11px] font-semibold transition ${
                              specialtyFilter === s 
                                ? 'bg-cyan-600 text-white' 
                                : 'bg-white border border-slate-200 text-slate-600'
                            }`}
                          >
                            {s}
                          </button>
                        ))}
                      </div>

                      {/* Doctor Cards */}
                      <div className="space-y-3 pt-1">
                        {filteredDoctors.map((doc) => (
                          <div 
                            key={doc.id}
                            className="bg-white p-3.5 rounded-2xl border border-slate-200 shadow-sm space-y-2.5"
                          >
                            <div className="flex items-start justify-between">
                              <div className="flex gap-3">
                                <div className="w-12 h-12 rounded-xl bg-cyan-100 text-cyan-700 font-bold flex items-center justify-center shrink-0">
                                  {doc.profile?.name.slice(3, 5).toUpperCase() || 'DR'}
                                </div>
                                <div>
                                  <h4 className="text-xs font-bold text-slate-900">{doc.profile?.name}</h4>
                                  <p className="text-[11px] font-semibold text-cyan-600">{doc.specialization}</p>
                                  <p className="text-[10px] text-slate-500">{doc.qualification}</p>
                                </div>
                              </div>
                              <span className={`text-[10px] px-2 py-0.5 rounded-md font-bold ${
                                doc.available ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'
                              }`}>
                                {doc.available ? 'Available' : 'Busy'}
                              </span>
                            </div>

                            <div className="flex items-center justify-between pt-1 border-t border-slate-100">
                              <span className="text-[11px] text-slate-400">⭐ {doc.rating} • {doc.experience}</span>
                              <div className="flex gap-2">
                                <button
                                  onClick={() => {
                                    setSelectedDoctor(doc);
                                    setCurrentScreen('doctor_details');
                                  }}
                                  className="px-2.5 py-1 text-[11px] font-bold text-slate-600 bg-slate-100 rounded-lg hover:bg-slate-200"
                                >
                                  Details
                                </button>
                                <button
                                  onClick={() => {
                                    setSelectedDoctor(doc);
                                    setCurrentScreen('book_appointment');
                                  }}
                                  className="px-3 py-1 text-[11px] font-bold text-white bg-cyan-600 rounded-lg hover:bg-cyan-700"
                                >
                                  Book
                                </button>
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* Bottom Nav Bar */}
                    <div className="bg-white border-t border-slate-200 px-6 py-2.5 flex justify-between items-center">
                      <button onClick={() => setCurrentScreen('patient_dashboard')} className="flex flex-col items-center text-slate-400 hover:text-slate-600">
                        <Heart className="w-5 h-5" />
                        <span className="text-[10px] font-medium mt-0.5">Home</span>
                      </button>
                      <button onClick={() => setCurrentScreen('doctor_list')} className="flex flex-col items-center text-cyan-600">
                        <Stethoscope className="w-5 h-5" />
                        <span className="text-[10px] font-bold mt-0.5">Doctors</span>
                      </button>
                      <button onClick={() => setCurrentScreen('my_appointments')} className="flex flex-col items-center text-slate-400 hover:text-slate-600">
                        <Calendar className="w-5 h-5" />
                        <span className="text-[10px] font-medium mt-0.5">Visits</span>
                      </button>
                      <button onClick={() => setCurrentScreen('patient_profile')} className="flex flex-col items-center text-slate-400 hover:text-slate-600">
                        <User className="w-5 h-5" />
                        <span className="text-[10px] font-medium mt-0.5">Profile</span>
                      </button>
                    </div>
                  </div>
                )}

                {/* 7. DOCTOR DETAILS */}
                {currentScreen === 'doctor_details' && (
                  <div className="flex-1 flex flex-col justify-between p-4 overflow-y-auto">
                    <div>
                      <div className="flex items-center gap-2 mb-3">
                        <button onClick={() => setCurrentScreen('doctor_list')} className="p-1 -ml-1 text-slate-500">
                          <ArrowLeft className="w-5 h-5" />
                        </button>
                        <h3 className="text-base font-bold text-slate-900">Doctor Profile</h3>
                      </div>

                      <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm text-center">
                        <div className="w-16 h-16 rounded-2xl bg-cyan-100 text-cyan-700 font-bold text-xl flex items-center justify-center mx-auto mb-3">
                          {selectedDoctor.profile?.name.slice(3, 5).toUpperCase() || 'DR'}
                        </div>
                        <h4 className="text-base font-bold text-slate-900">{selectedDoctor.profile?.name}</h4>
                        <p className="text-xs font-semibold text-cyan-600 mt-0.5">{selectedDoctor.specialization}</p>
                        <p className="text-[11px] text-slate-500 mt-1">{selectedDoctor.qualification}</p>

                        <div className="grid grid-cols-3 gap-2 mt-4 pt-4 border-t border-slate-100">
                          <div>
                            <p className="text-xs font-bold text-cyan-700">{selectedDoctor.experience}</p>
                            <p className="text-[10px] text-slate-400">Experience</p>
                          </div>
                          <div>
                            <p className="text-xs font-bold text-cyan-700">⭐ {selectedDoctor.rating}</p>
                            <p className="text-[10px] text-slate-400">Rating</p>
                          </div>
                          <div>
                            <p className="text-xs font-bold text-cyan-700">950+</p>
                            <p className="text-[10px] text-slate-400">Patients</p>
                          </div>
                        </div>
                      </div>

                      <div className="mt-4 space-y-2">
                        <h5 className="text-xs font-bold text-slate-900">About Doctor</h5>
                        <p className="text-xs text-slate-600 leading-relaxed bg-white p-3 rounded-2xl border border-slate-200">
                          {selectedDoctor.about}
                        </p>
                      </div>

                      <div className="mt-4 space-y-2">
                        <h5 className="text-xs font-bold text-slate-900">Consultation Hours</h5>
                        <div className="bg-white p-3 rounded-2xl border border-slate-200 text-xs text-slate-700">
                          <p className="font-semibold">Mon - Fri: 09:00 AM - 05:00 PM</p>
                          <p className="text-[11px] text-slate-400 mt-0.5">Saturday: 10:00 AM - 02:00 PM (Emergency on-call)</p>
                        </div>
                      </div>
                    </div>

                    <button
                      onClick={() => setCurrentScreen('book_appointment')}
                      className="w-full bg-cyan-600 hover:bg-cyan-700 text-white font-bold py-3.5 rounded-xl shadow-md transition mt-4"
                    >
                      BOOK APPOINTMENT
                    </button>
                  </div>
                )}

                {/* 8. BOOK APPOINTMENT */}
                {currentScreen === 'book_appointment' && (
                  <div className="flex-1 flex flex-col justify-between p-4 overflow-y-auto">
                    <div>
                      <div className="flex items-center gap-2 mb-3">
                        <button onClick={() => setCurrentScreen('doctor_list')} className="p-1 -ml-1 text-slate-500">
                          <ArrowLeft className="w-5 h-5" />
                        </button>
                        <h3 className="text-base font-bold text-slate-900">Book Consultation</h3>
                      </div>

                      {/* Doctor Mini Card */}
                      <div className="bg-white p-3 rounded-2xl border border-slate-200 flex items-center gap-3 mb-4">
                        <div className="w-10 h-10 rounded-xl bg-cyan-100 text-cyan-700 font-bold flex items-center justify-center">
                          {selectedDoctor.profile?.name.slice(3, 5).toUpperCase() || 'DR'}
                        </div>
                        <div>
                          <h4 className="text-xs font-bold text-slate-900">{selectedDoctor.profile?.name}</h4>
                          <p className="text-[11px] text-cyan-600">{selectedDoctor.specialization}</p>
                        </div>
                      </div>

                      {/* Date Selection */}
                      <div className="space-y-1.5 mb-4">
                        <label className="text-xs font-bold text-slate-700">Select Date</label>
                        <input 
                          type="date" 
                          value={bookingDate}
                          onChange={(e) => setBookingDate(e.target.value)}
                          className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs font-semibold focus:outline-none bg-white"
                        />
                      </div>

                      {/* Time Slots */}
                      <div className="space-y-1.5 mb-4">
                        <label className="text-xs font-bold text-slate-700">Select Time Slot</label>
                        <div className="grid grid-cols-2 gap-2">
                          {['09:30 AM', '11:00 AM', '02:30 PM', '04:15 PM'].map((time) => (
                            <button
                              key={time}
                              onClick={() => setBookingTime(time)}
                              className={`py-2 rounded-xl text-xs font-bold transition ${
                                bookingTime === time 
                                  ? 'bg-cyan-600 text-white' 
                                  : 'bg-white border border-slate-200 text-slate-600'
                              }`}
                            >
                              {time}
                            </button>
                          ))}
                        </div>
                      </div>

                      {/* Reason */}
                      <div className="space-y-1.5 mb-4">
                        <label className="text-xs font-bold text-slate-700">Reason for Visit</label>
                        <textarea
                          rows={3}
                          value={bookingReason}
                          onChange={(e) => setBookingReason(e.target.value)}
                          placeholder="Describe symptoms or medical reason..."
                          className="w-full p-3 rounded-xl border border-slate-200 text-xs focus:outline-none bg-white resize-none"
                        />
                      </div>
                    </div>

                    <button
                      onClick={handleBookAppointment}
                      className="w-full bg-cyan-600 hover:bg-cyan-700 text-white font-bold py-3.5 rounded-xl shadow-md transition"
                    >
                      CONFIRM BOOKING
                    </button>
                  </div>
                )}

                {/* 9. MY APPOINTMENTS */}
                {currentScreen === 'my_appointments' && (
                  <div className="flex-1 flex flex-col justify-between">
                    <div className="p-4 space-y-3 overflow-y-auto">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <button onClick={() => setCurrentScreen('patient_dashboard')} className="p-1 -ml-1 text-slate-500">
                            <ArrowLeft className="w-5 h-5" />
                          </button>
                          <h3 className="text-base font-bold text-slate-900">My Appointments</h3>
                        </div>
                        <span className="text-[11px] font-semibold text-slate-500">
                          {userAppointments.length} Booked
                        </span>
                      </div>

                      {userAppointments.length === 0 ? (
                        <div className="text-center py-12">
                          <Calendar className="w-12 h-12 text-slate-300 mx-auto mb-2" />
                          <p className="text-xs text-slate-500">No appointments scheduled</p>
                          <button 
                            onClick={() => setCurrentScreen('doctor_list')}
                            className="mt-3 text-xs font-bold text-cyan-600"
                          >
                            Book a doctor now →
                          </button>
                        </div>
                      ) : (
                        <div className="space-y-3">
                          {userAppointments.map((apt) => (
                            <div 
                              key={apt.id}
                              className="bg-white p-3.5 rounded-2xl border border-slate-200 shadow-sm space-y-2"
                            >
                              <div className="flex items-start justify-between">
                                <div>
                                  <h4 className="text-xs font-bold text-slate-900">{apt.doctor?.profile?.name || 'Dr. Specialist'}</h4>
                                  <p className="text-[11px] font-semibold text-cyan-600">{apt.doctor?.specialization}</p>
                                </div>
                                <span className={`text-[10px] px-2 py-0.5 rounded-md font-bold uppercase ${
                                  apt.status === 'confirmed' ? 'bg-emerald-100 text-emerald-700' :
                                  apt.status === 'pending' ? 'bg-amber-100 text-amber-700' :
                                  apt.status === 'completed' ? 'bg-blue-100 text-blue-700' :
                                  'bg-slate-100 text-slate-600'
                                }`}>
                                  {apt.status}
                                </span>
                              </div>

                              <div className="text-[11px] text-slate-500 flex items-center gap-3">
                                <span>📅 {apt.appointment_date}</span>
                                <span>⏰ {apt.appointment_time}</span>
                              </div>

                              <p className="text-[11px] text-slate-600 bg-slate-50 p-2 rounded-xl border border-slate-100">
                                {apt.reason}
                              </p>

                              <div className="flex items-center justify-end gap-2 pt-1">
                                {apt.status === 'completed' && (
                                  <button
                                    onClick={() => {
                                      const rx = prescriptions.find(p => p.appointment_id === apt.id) || prescriptions[0];
                                      setSelectedPrescription(rx);
                                      setCurrentScreen('prescription_details');
                                    }}
                                    className="px-2.5 py-1 text-[11px] font-bold text-blue-700 bg-blue-50 rounded-lg hover:bg-blue-100"
                                  >
                                    View Prescription 💊
                                  </button>
                                )}
                                {(apt.status === 'pending' || apt.status === 'confirmed') && (
                                  <button
                                    onClick={() => handleUpdateAppointmentStatus(apt.id, 'cancelled')}
                                    className="px-2.5 py-1 text-[11px] font-bold text-rose-700 bg-rose-50 rounded-lg hover:bg-rose-100"
                                  >
                                    Cancel
                                  </button>
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>

                    {/* Bottom Nav Bar */}
                    <div className="bg-white border-t border-slate-200 px-6 py-2.5 flex justify-between items-center">
                      <button onClick={() => setCurrentScreen('patient_dashboard')} className="flex flex-col items-center text-slate-400 hover:text-slate-600">
                        <Heart className="w-5 h-5" />
                        <span className="text-[10px] font-medium mt-0.5">Home</span>
                      </button>
                      <button onClick={() => setCurrentScreen('doctor_list')} className="flex flex-col items-center text-slate-400 hover:text-slate-600">
                        <Stethoscope className="w-5 h-5" />
                        <span className="text-[10px] font-medium mt-0.5">Doctors</span>
                      </button>
                      <button onClick={() => setCurrentScreen('my_appointments')} className="flex flex-col items-center text-cyan-600">
                        <Calendar className="w-5 h-5" />
                        <span className="text-[10px] font-bold mt-0.5">Visits</span>
                      </button>
                      <button onClick={() => setCurrentScreen('patient_profile')} className="flex flex-col items-center text-slate-400 hover:text-slate-600">
                        <User className="w-5 h-5" />
                        <span className="text-[10px] font-medium mt-0.5">Profile</span>
                      </button>
                    </div>
                  </div>
                )}

                {/* 10. PRESCRIPTION DETAILS */}
                {currentScreen === 'prescription_details' && selectedPrescription && (
                  <div className="flex-1 flex flex-col justify-between p-4 overflow-y-auto">
                    <div>
                      <div className="flex items-center gap-2 mb-3">
                        <button onClick={() => setCurrentScreen('my_appointments')} className="p-1 -ml-1 text-slate-500">
                          <ArrowLeft className="w-5 h-5" />
                        </button>
                        <h3 className="text-base font-bold text-slate-900">Medical Prescription</h3>
                      </div>

                      <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm space-y-4">
                        <div className="flex items-center gap-3 pb-3 border-b border-slate-100">
                          <div className="w-12 h-12 rounded-2xl bg-cyan-600 text-white font-extrabold text-lg flex items-center justify-center">
                            Rx
                          </div>
                          <div>
                            <h4 className="text-xs font-bold text-slate-900">{selectedPrescription.doctor_name || 'Dr. Specialist'}</h4>
                            <p className="text-[11px] text-cyan-600">{selectedPrescription.doctor_specialty || 'General Consultant'}</p>
                          </div>
                        </div>

                        <div className="space-y-1">
                          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Medication</span>
                          <p className="text-sm font-bold text-slate-900">{selectedPrescription.medicine}</p>
                        </div>

                        <div className="grid grid-cols-2 gap-3 pt-2">
                          <div className="bg-slate-50 p-2.5 rounded-xl border border-slate-100">
                            <span className="text-[10px] text-slate-400">Dosage</span>
                            <p className="text-xs font-bold text-slate-800 mt-0.5">{selectedPrescription.dosage}</p>
                          </div>
                          <div className="bg-slate-50 p-2.5 rounded-xl border border-slate-100">
                            <span className="text-[10px] text-slate-400">Frequency</span>
                            <p className="text-xs font-bold text-slate-800 mt-0.5">{selectedPrescription.frequency}</p>
                          </div>
                        </div>

                        <div className="space-y-1 pt-1">
                          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Special Instructions</span>
                          <p className="text-xs text-slate-600 leading-relaxed bg-slate-50 p-3 rounded-xl border border-slate-100">
                            {selectedPrescription.instructions}
                          </p>
                        </div>

                        <div className="pt-2 text-center">
                          <span className="text-[10px] text-emerald-700 bg-emerald-50 px-3 py-1 rounded-full font-bold">
                            ✓ Verified Supabase Digital Prescription
                          </span>
                        </div>
                      </div>
                    </div>

                    <button
                      onClick={() => setCurrentScreen('my_appointments')}
                      className="w-full bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold py-3.5 rounded-xl transition mt-4"
                    >
                      BACK TO VISITS
                    </button>
                  </div>
                )}

                {/* 11. PATIENT PROFILE */}
                {currentScreen === 'patient_profile' && (
                  <div className="flex-1 flex flex-col justify-between p-4 overflow-y-auto">
                    <div>
                      <div className="flex items-center gap-2 mb-3">
                        <button onClick={() => setCurrentScreen('patient_dashboard')} className="p-1 -ml-1 text-slate-500">
                          <ArrowLeft className="w-5 h-5" />
                        </button>
                        <h3 className="text-base font-bold text-slate-900">My Profile</h3>
                      </div>

                      <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm text-center mb-4">
                        <div className="w-16 h-16 rounded-full bg-cyan-100 text-cyan-700 font-bold text-xl flex items-center justify-center mx-auto mb-2 border border-cyan-200">
                          {currentUser.name.slice(0, 2).toUpperCase()}
                        </div>
                        <h4 className="text-base font-bold text-slate-900">{currentUser.name}</h4>
                        <span className="text-[10px] uppercase font-bold px-2 py-0.5 bg-emerald-100 text-emerald-700 rounded-md">
                          Patient
                        </span>
                      </div>

                      <div className="bg-white p-4 rounded-3xl border border-slate-200 space-y-3 text-xs">
                        <div>
                          <span className="text-slate-400">Email</span>
                          <p className="font-semibold text-slate-800">{currentUser.email}</p>
                        </div>
                        <div>
                          <span className="text-slate-400">Phone</span>
                          <p className="font-semibold text-slate-800">{currentUser.phone || '+1-555-0201'}</p>
                        </div>
                        <div className="flex justify-between border-t border-slate-100 pt-2">
                          <div>
                            <span className="text-slate-400">Blood Group</span>
                            <p className="font-bold text-cyan-700">O+</p>
                          </div>
                          <div>
                            <span className="text-slate-400">Gender</span>
                            <p className="font-semibold text-slate-800">Male</p>
                          </div>
                        </div>
                      </div>
                    </div>

                    <button
                      onClick={() => {
                        showToast('Logged out');
                        setCurrentScreen('welcome');
                      }}
                      className="w-full bg-rose-50 hover:bg-rose-100 text-rose-700 font-bold py-3.5 rounded-xl border border-rose-200 transition"
                    >
                      LOG OUT
                    </button>
                  </div>
                )}

                {/* 12. DOCTOR DASHBOARD */}
                {currentScreen === 'doctor_dashboard' && (
                  <div className="flex-1 flex flex-col justify-between p-4 overflow-y-auto">
                    <div className="space-y-4">
                      <div className="flex items-center justify-between">
                        <div>
                          <h3 className="text-lg font-bold text-slate-900">Dr. Dashboard</h3>
                          <p className="text-xs text-cyan-600 font-semibold">{currentUser.name} • Online</p>
                        </div>
                        <button 
                          onClick={() => setCurrentScreen('doctor_profile')}
                          className="w-10 h-10 rounded-full bg-cyan-100 text-cyan-700 font-bold flex items-center justify-center border border-cyan-200"
                        >
                          DR
                        </button>
                      </div>

                      {/* 3 Metric Stats */}
                      <div className="grid grid-cols-3 gap-2">
                        <div className="bg-amber-50 border border-amber-100 p-3 rounded-2xl text-center">
                          <p className="text-xl font-bold text-amber-600">
                            {appointments.filter(a => a.status === 'pending').length}
                          </p>
                          <span className="text-[10px] text-amber-800 font-medium">Pending</span>
                        </div>
                        <div className="bg-emerald-50 border border-emerald-100 p-3 rounded-2xl text-center">
                          <p className="text-xl font-bold text-emerald-600">
                            {appointments.filter(a => a.status === 'confirmed').length}
                          </p>
                          <span className="text-[10px] text-emerald-800 font-medium">Confirmed</span>
                        </div>
                        <div className="bg-blue-50 border border-blue-100 p-3 rounded-2xl text-center">
                          <p className="text-xl font-bold text-blue-600">
                            {appointments.filter(a => a.status === 'completed').length}
                          </p>
                          <span className="text-[10px] text-blue-800 font-medium">Completed</span>
                        </div>
                      </div>

                      {/* Today's Appointments List */}
                      <div>
                        <div className="flex justify-between items-center mb-2">
                          <span className="text-xs font-bold text-slate-900">Patient Consultations</span>
                          <span className="text-[11px] text-slate-400">{appointments.length} total</span>
                        </div>
                        <div className="space-y-3">
                          {appointments.map((apt) => (
                            <div key={apt.id} className="bg-white p-3.5 rounded-2xl border border-slate-200 shadow-sm space-y-2">
                              <div className="flex items-start justify-between">
                                <div>
                                  <h4 className="text-xs font-bold text-slate-900">{apt.patient?.profile?.name || 'Alex Johnson'}</h4>
                                  <p className="text-[10px] text-slate-400">📅 {apt.appointment_date} • ⏰ {apt.appointment_time}</p>
                                </div>
                                <span className={`text-[10px] px-2 py-0.5 rounded-md font-bold uppercase ${
                                  apt.status === 'confirmed' ? 'bg-emerald-100 text-emerald-700' :
                                  apt.status === 'pending' ? 'bg-amber-100 text-amber-700' :
                                  apt.status === 'completed' ? 'bg-blue-100 text-blue-700' :
                                  'bg-slate-100 text-slate-600'
                                }`}>
                                  {apt.status}
                                </span>
                              </div>

                              <p className="text-[11px] text-slate-600 bg-slate-50 p-2 rounded-xl">
                                {apt.reason}
                              </p>

                              {/* Doctor Actions */}
                              <div className="flex items-center justify-end gap-2 pt-1">
                                {apt.status === 'pending' && (
                                  <>
                                    <button
                                      onClick={() => handleUpdateAppointmentStatus(apt.id, 'rejected')}
                                      className="px-2.5 py-1 text-[11px] font-bold text-rose-700 bg-rose-50 rounded-lg hover:bg-rose-100"
                                    >
                                      Reject
                                    </button>
                                    <button
                                      onClick={() => handleUpdateAppointmentStatus(apt.id, 'confirmed')}
                                      className="px-3 py-1 text-[11px] font-bold text-white bg-emerald-600 rounded-lg hover:bg-emerald-700"
                                    >
                                      Accept
                                    </button>
                                  </>
                                )}
                                {apt.status === 'confirmed' && (
                                  <button
                                    onClick={() => handleUpdateAppointmentStatus(apt.id, 'completed')}
                                    className="px-3 py-1 text-[11px] font-bold text-white bg-cyan-600 rounded-lg hover:bg-cyan-700"
                                  >
                                    Mark Completed
                                  </button>
                                )}
                                {apt.status === 'completed' && (
                                  <button
                                    onClick={() => {
                                      setSelectedAppointment(apt);
                                      setCurrentScreen('add_prescription');
                                    }}
                                    className="px-3 py-1 text-[11px] font-bold text-blue-700 bg-blue-50 rounded-lg hover:bg-blue-100"
                                  >
                                    + Add Prescription 💊
                                  </button>
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>

                    <div className="pt-4 border-t border-slate-200 mt-4">
                      <button
                        onClick={() => setCurrentScreen('doctor_profile')}
                        className="w-full bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold py-2.5 rounded-xl text-xs"
                      >
                        Doctor Settings &amp; Profile
                      </button>
                    </div>
                  </div>
                )}

                {/* 13. ADD PRESCRIPTION (DOCTOR) */}
                {currentScreen === 'add_prescription' && (
                  <div className="flex-1 flex flex-col justify-between p-4 overflow-y-auto">
                    <div>
                      <div className="flex items-center gap-2 mb-3">
                        <button onClick={() => setCurrentScreen('doctor_dashboard')} className="p-1 -ml-1 text-slate-500">
                          <ArrowLeft className="w-5 h-5" />
                        </button>
                        <h3 className="text-base font-bold text-slate-900">Add Prescription</h3>
                      </div>

                      <div className="bg-cyan-50 p-3 rounded-2xl border border-cyan-100 text-xs text-cyan-800 mb-4">
                        Patient: <strong>{selectedAppointment?.patient?.profile?.name || 'Alex Johnson'}</strong>
                      </div>

                      <div className="space-y-3">
                        <div>
                          <label className="text-xs font-bold text-slate-700 block mb-1">Medicine Name</label>
                          <input
                            value={rxMedicine}
                            onChange={(e) => setRxMedicine(e.target.value)}
                            placeholder="e.g. Amoxicillin, Paracetamol"
                            className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none bg-white"
                          />
                        </div>
                        <div>
                          <label className="text-xs font-bold text-slate-700 block mb-1">Dosage</label>
                          <input
                            value={rxDosage}
                            onChange={(e) => setRxDosage(e.target.value)}
                            placeholder="e.g. 500mg, 1 Capsule"
                            className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none bg-white"
                          />
                        </div>
                        <div>
                          <label className="text-xs font-bold text-slate-700 block mb-1">Frequency</label>
                          <input
                            value={rxFrequency}
                            onChange={(e) => setRxFrequency(e.target.value)}
                            placeholder="e.g. Twice daily after meals"
                            className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none bg-white"
                          />
                        </div>
                        <div>
                          <label className="text-xs font-bold text-slate-700 block mb-1">Special Instructions</label>
                          <textarea
                            rows={3}
                            value={rxInstructions}
                            onChange={(e) => setRxInstructions(e.target.value)}
                            placeholder="Precautions, dietary notes..."
                            className="w-full p-2.5 rounded-xl border border-slate-200 text-xs focus:outline-none bg-white resize-none"
                          />
                        </div>
                      </div>
                    </div>

                    <button
                      onClick={handleSavePrescription}
                      className="w-full bg-cyan-600 hover:bg-cyan-700 text-white font-bold py-3.5 rounded-xl shadow-md transition mt-4"
                    >
                      SAVE PRESCRIPTION
                    </button>
                  </div>
                )}

                {/* 14. DOCTOR PROFILE */}
                {currentScreen === 'doctor_profile' && (
                  <div className="flex-1 flex flex-col justify-between p-4 overflow-y-auto">
                    <div>
                      <div className="flex items-center gap-2 mb-3">
                        <button onClick={() => setCurrentScreen('doctor_dashboard')} className="p-1 -ml-1 text-slate-500">
                          <ArrowLeft className="w-5 h-5" />
                        </button>
                        <h3 className="text-base font-bold text-slate-900">Doctor Profile</h3>
                      </div>

                      <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm text-center mb-4">
                        <div className="w-16 h-16 rounded-2xl bg-cyan-100 text-cyan-700 font-bold text-xl flex items-center justify-center mx-auto mb-2">
                          DR
                        </div>
                        <h4 className="text-base font-bold text-slate-900">{currentUser.name}</h4>
                        <span className="text-[10px] uppercase font-bold px-2 py-0.5 bg-cyan-100 text-cyan-700 rounded-md">
                          Cardiologist • Senior Consultant
                        </span>
                      </div>

                      <div className="bg-white p-4 rounded-3xl border border-slate-200 space-y-3 text-xs">
                        <div>
                          <span className="text-slate-400">Email</span>
                          <p className="font-semibold text-slate-800">{currentUser.email}</p>
                        </div>
                        <div>
                          <span className="text-slate-400">Qualification</span>
                          <p className="font-semibold text-slate-800">MBBS, MD (Cardiology), FACC</p>
                        </div>
                        <div>
                          <span className="text-slate-400">Experience</span>
                          <p className="font-semibold text-slate-800">12 Years in Clinical Practice</p>
                        </div>
                      </div>
                    </div>

                    <button
                      onClick={() => {
                        showToast('Signed out of doctor portal');
                        setCurrentScreen('welcome');
                      }}
                      className="w-full bg-rose-50 hover:bg-rose-100 text-rose-700 font-bold py-3.5 rounded-xl border border-rose-200 transition"
                    >
                      LOG OUT
                    </button>
                  </div>
                )}

                {/* 15. ADMIN DASHBOARD */}
                {currentScreen === 'admin_dashboard' && (
                  <div className="flex-1 flex flex-col justify-between p-4 overflow-y-auto">
                    <div className="space-y-4">
                      <div>
                        <h3 className="text-lg font-bold text-slate-900">Admin Control Center</h3>
                        <p className="text-xs text-purple-600 font-semibold">Hospital Management Console</p>
                      </div>

                      {/* 4 Stats */}
                      <div className="grid grid-cols-2 gap-2.5">
                        <div className="bg-cyan-50 border border-cyan-100 p-3.5 rounded-2xl">
                          <p className="text-2xl font-bold text-cyan-700">{doctors.length}</p>
                          <span className="text-[11px] font-semibold text-cyan-900">Total Doctors</span>
                        </div>
                        <div className="bg-emerald-50 border border-emerald-100 p-3.5 rounded-2xl">
                          <p className="text-2xl font-bold text-emerald-700">{patients.length}</p>
                          <span className="text-[11px] font-semibold text-emerald-900">Total Patients</span>
                        </div>
                        <div className="bg-blue-50 border border-blue-100 p-3.5 rounded-2xl">
                          <p className="text-2xl font-bold text-blue-700">{appointments.length}</p>
                          <span className="text-[11px] font-semibold text-blue-900">Appointments</span>
                        </div>
                        <div className="bg-amber-50 border border-amber-100 p-3.5 rounded-2xl">
                          <p className="text-2xl font-bold text-amber-700">
                            {appointments.filter(a => a.status === 'pending').length}
                          </p>
                          <span className="text-[11px] font-semibold text-amber-900">Pending Action</span>
                        </div>
                      </div>

                      {/* Admin Links */}
                      <div className="space-y-2 pt-2">
                        <button
                          onClick={() => setCurrentScreen('admin_manage_doctors')}
                          className="w-full bg-white p-3.5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between text-left hover:border-purple-300 transition"
                        >
                          <div className="flex items-center gap-3">
                            <span className="text-xl">👨‍⚕️</span>
                            <div>
                              <p className="text-xs font-bold text-slate-900">Manage Doctors</p>
                              <p className="text-[10px] text-slate-400">Add physicians, specialties, availability</p>
                            </div>
                          </div>
                          <ChevronRight className="w-4 h-4 text-slate-400" />
                        </button>

                        <button
                          onClick={() => setCurrentScreen('admin_manage_patients')}
                          className="w-full bg-white p-3.5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between text-left hover:border-purple-300 transition"
                        >
                          <div className="flex items-center gap-3">
                            <span className="text-xl">🏥</span>
                            <div>
                              <p className="text-xs font-bold text-slate-900">Manage Patients</p>
                              <p className="text-[10px] text-slate-400">View demographics, blood groups, history</p>
                            </div>
                          </div>
                          <ChevronRight className="w-4 h-4 text-slate-400" />
                        </button>
                      </div>
                    </div>

                    <button
                      onClick={() => {
                        showToast('Exited Admin session');
                        setCurrentScreen('welcome');
                      }}
                      className="w-full bg-rose-50 hover:bg-rose-100 text-rose-700 font-bold py-3.5 rounded-xl border border-rose-200 transition"
                    >
                      LOG OUT
                    </button>
                  </div>
                )}

                {/* 16. ADMIN MANAGE DOCTORS */}
                {currentScreen === 'admin_manage_doctors' && (
                  <div className="flex-1 flex flex-col justify-between p-4 overflow-y-auto">
                    <div>
                      <div className="flex items-center justify-between mb-3">
                        <div className="flex items-center gap-2">
                          <button onClick={() => setCurrentScreen('admin_dashboard')} className="p-1 -ml-1 text-slate-500">
                            <ArrowLeft className="w-5 h-5" />
                          </button>
                          <h3 className="text-base font-bold text-slate-900">Manage Doctors</h3>
                        </div>
                        <button
                          onClick={() => setCurrentScreen('admin_add_doctor')}
                          className="px-2.5 py-1 bg-cyan-600 text-white rounded-lg text-xs font-bold"
                        >
                          + Add Doc
                        </button>
                      </div>

                      <div className="space-y-2.5">
                        {doctors.map((doc) => (
                          <div key={doc.id} className="bg-white p-3 rounded-2xl border border-slate-200 flex items-center justify-between">
                            <div>
                              <h4 className="text-xs font-bold text-slate-900">{doc.profile?.name}</h4>
                              <p className="text-[11px] text-cyan-600 font-semibold">{doc.specialization}</p>
                              <p className="text-[10px] text-slate-400">{doc.qualification}</p>
                            </div>
                            <button
                              onClick={() => handleDeleteDoctor(doc.id)}
                              className="px-2.5 py-1 text-[11px] font-bold text-rose-600 bg-rose-50 rounded-lg hover:bg-rose-100"
                            >
                              Delete
                            </button>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                )}

                {/* 17. ADMIN ADD DOCTOR */}
                {currentScreen === 'admin_add_doctor' && (
                  <div className="flex-1 flex flex-col justify-between p-4 overflow-y-auto">
                    <div>
                      <div className="flex items-center gap-2 mb-3">
                        <button onClick={() => setCurrentScreen('admin_manage_doctors')} className="p-1 -ml-1 text-slate-500">
                          <ArrowLeft className="w-5 h-5" />
                        </button>
                        <h3 className="text-base font-bold text-slate-900">Add Physician</h3>
                      </div>

                      <div className="space-y-2.5">
                        <div>
                          <label className="text-[11px] font-bold text-slate-700 block mb-0.5">Doctor Full Name</label>
                          <input
                            value={newDocName}
                            onChange={(e) => setNewDocName(e.target.value)}
                            placeholder="e.g. Dr. Emily Watson"
                            className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none bg-white"
                          />
                        </div>
                        <div>
                          <label className="text-[11px] font-bold text-slate-700 block mb-0.5">Email</label>
                          <input
                            value={newDocEmail}
                            onChange={(e) => setNewDocEmail(e.target.value)}
                            placeholder="e.watson@medicare.com"
                            className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none bg-white"
                          />
                        </div>
                        <div>
                          <label className="text-[11px] font-bold text-slate-700 block mb-0.5">Specialization</label>
                          <input
                            value={newDocSpecialty}
                            onChange={(e) => setNewDocSpecialty(e.target.value)}
                            placeholder="e.g. Dermatologist, Oncologist"
                            className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none bg-white"
                          />
                        </div>
                        <div>
                          <label className="text-[11px] font-bold text-slate-700 block mb-0.5">Qualification</label>
                          <input
                            value={newDocQual}
                            onChange={(e) => setNewDocQual(e.target.value)}
                            className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none bg-white"
                          />
                        </div>
                        <div>
                          <label className="text-[11px] font-bold text-slate-700 block mb-0.5">Experience</label>
                          <input
                            value={newDocExp}
                            onChange={(e) => setNewDocExp(e.target.value)}
                            className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none bg-white"
                          />
                        </div>
                      </div>
                    </div>

                    <button
                      onClick={handleAdminAddDoctor}
                      className="w-full bg-cyan-600 hover:bg-cyan-700 text-white font-bold py-3.5 rounded-xl shadow-md transition mt-4"
                    >
                      ENROLL DOCTOR
                    </button>
                  </div>
                )}

                {/* 18. ADMIN MANAGE PATIENTS */}
                {currentScreen === 'admin_manage_patients' && (
                  <div className="flex-1 flex flex-col justify-between p-4 overflow-y-auto">
                    <div>
                      <div className="flex items-center gap-2 mb-3">
                        <button onClick={() => setCurrentScreen('admin_dashboard')} className="p-1 -ml-1 text-slate-500">
                          <ArrowLeft className="w-5 h-5" />
                        </button>
                        <h3 className="text-base font-bold text-slate-900">Patient Registry</h3>
                      </div>

                      <div className="space-y-2.5">
                        {patients.map((pat) => (
                          <div key={pat.id} className="bg-white p-3 rounded-2xl border border-slate-200">
                            <div className="flex justify-between items-start">
                              <div>
                                <h4 className="text-xs font-bold text-slate-900">{pat.profile?.name || 'Patient'}</h4>
                                <p className="text-[11px] text-slate-400">{pat.profile?.email}</p>
                              </div>
                              <span className="text-[10px] px-2 py-0.5 rounded font-bold bg-cyan-100 text-cyan-800">
                                Blood: {pat.blood_group || 'O+'}
                              </span>
                            </div>
                            <p className="text-[11px] text-slate-500 mt-1 bg-slate-50 p-2 rounded-xl">
                              Notes: {pat.medical_history || 'No recorded chronic conditions.'}
                            </p>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                )}

              </div>

              {/* Android Home Indicator Bar */}
              <div className="bg-white py-1.5 flex justify-center">
                <div className="w-32 h-1 bg-slate-300 rounded-full" />
              </div>
            </div>
          </div>
        )}

        {/* SUPABASE LIVE DATABASE EXPLORER TAB */}
        {activeTab === 'database' && (
          <div className="w-full max-w-5xl bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-2xl space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b border-slate-800">
              <div>
                <div className="flex items-center gap-2">
                  <Database className="w-5 h-5 text-emerald-400" />
                  <h2 className="text-lg font-bold text-white">Supabase PostgreSQL Live Tables</h2>
                </div>
                <p className="text-xs text-slate-400">
                  Data synchronizes in real-time as you book appointments, issue prescriptions, and update records in the simulator.
                </p>
              </div>

              <div className="flex gap-1.5 bg-slate-950 p-1 rounded-xl border border-slate-800">
                {(['appointments', 'doctors', 'patients', 'prescriptions', 'profiles'] as const).map((t) => (
                  <button
                    key={t}
                    onClick={() => setDbTable(t)}
                    className={`px-3 py-1 rounded-lg text-xs font-bold capitalize transition ${
                      dbTable === t ? 'bg-emerald-600 text-white' : 'text-slate-400 hover:text-slate-200'
                    }`}
                  >
                    {t}
                  </button>
                ))}
              </div>
            </div>

            {/* Table Display */}
            <div className="overflow-x-auto rounded-xl border border-slate-800 bg-slate-950">
              {dbTable === 'appointments' && (
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="bg-slate-900 text-slate-400 uppercase font-semibold text-[10px]">
                    <tr>
                      <th className="p-3">ID</th>
                      <th className="p-3">Doctor</th>
                      <th className="p-3">Patient</th>
                      <th className="p-3">Date &amp; Time</th>
                      <th className="p-3">Reason</th>
                      <th className="p-3">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {appointments.map((a) => (
                      <tr key={a.id} className="hover:bg-slate-900/40">
                        <td className="p-3 font-mono text-cyan-400">{a.id}</td>
                        <td className="p-3 font-bold text-white">{a.doctor?.profile?.name || a.doctor_id}</td>
                        <td className="p-3">{a.patient?.profile?.name || a.patient_id}</td>
                        <td className="p-3">{a.appointment_date} {a.appointment_time}</td>
                        <td className="p-3 max-w-xs truncate">{a.reason}</td>
                        <td className="p-3">
                          <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                            a.status === 'confirmed' ? 'bg-emerald-950 text-emerald-400 border border-emerald-800' :
                            a.status === 'pending' ? 'bg-amber-950 text-amber-400 border border-amber-800' :
                            a.status === 'completed' ? 'bg-blue-950 text-blue-400 border border-blue-800' :
                            'bg-slate-800 text-slate-400'
                          }`}>
                            {a.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              {dbTable === 'doctors' && (
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="bg-slate-900 text-slate-400 uppercase font-semibold text-[10px]">
                    <tr>
                      <th className="p-3">Doctor ID</th>
                      <th className="p-3">Name</th>
                      <th className="p-3">Specialization</th>
                      <th className="p-3">Qualification</th>
                      <th className="p-3">Experience</th>
                      <th className="p-3">Availability</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {doctors.map((d) => (
                      <tr key={d.id} className="hover:bg-slate-900/40">
                        <td className="p-3 font-mono text-cyan-400">{d.id}</td>
                        <td className="p-3 font-bold text-white">{d.profile?.name}</td>
                        <td className="p-3 text-cyan-400">{d.specialization}</td>
                        <td className="p-3">{d.qualification}</td>
                        <td className="p-3">{d.experience}</td>
                        <td className="p-3">
                          <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                            d.available ? 'bg-emerald-950 text-emerald-400' : 'bg-rose-950 text-rose-400'
                          }`}>
                            {d.available ? 'Active' : 'Unavailable'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              {dbTable === 'patients' && (
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="bg-slate-900 text-slate-400 uppercase font-semibold text-[10px]">
                    <tr>
                      <th className="p-3">Patient ID</th>
                      <th className="p-3">Name</th>
                      <th className="p-3">Blood Group</th>
                      <th className="p-3">Gender</th>
                      <th className="p-3">Medical History</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {patients.map((p) => (
                      <tr key={p.id} className="hover:bg-slate-900/40">
                        <td className="p-3 font-mono text-cyan-400">{p.id}</td>
                        <td className="p-3 font-bold text-white">{p.profile?.name}</td>
                        <td className="p-3 text-rose-400 font-bold">{p.blood_group}</td>
                        <td className="p-3">{p.gender}</td>
                        <td className="p-3 max-w-sm truncate">{p.medical_history}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              {dbTable === 'prescriptions' && (
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="bg-slate-900 text-slate-400 uppercase font-semibold text-[10px]">
                    <tr>
                      <th className="p-3">Rx ID</th>
                      <th className="p-3">Appointment</th>
                      <th className="p-3">Medicine</th>
                      <th className="p-3">Dosage</th>
                      <th className="p-3">Frequency</th>
                      <th className="p-3">Special Instructions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {prescriptions.map((rx) => (
                      <tr key={rx.id} className="hover:bg-slate-900/40">
                        <td className="p-3 font-mono text-cyan-400">{rx.id}</td>
                        <td className="p-3 font-mono text-slate-400">{rx.appointment_id}</td>
                        <td className="p-3 font-bold text-white">{rx.medicine}</td>
                        <td className="p-3">{rx.dosage}</td>
                        <td className="p-3 text-cyan-400">{rx.frequency}</td>
                        <td className="p-3 max-w-sm truncate">{rx.instructions}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              {dbTable === 'profiles' && (
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="bg-slate-900 text-slate-400 uppercase font-semibold text-[10px]">
                    <tr>
                      <th className="p-3">User ID</th>
                      <th className="p-3">Email</th>
                      <th className="p-3">Name</th>
                      <th className="p-3">Role</th>
                      <th className="p-3">Phone</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {profiles.map((prof) => (
                      <tr key={prof.id} className="hover:bg-slate-900/40">
                        <td className="p-3 font-mono text-cyan-400">{prof.id}</td>
                        <td className="p-3">{prof.email}</td>
                        <td className="p-3 font-bold text-white">{prof.name}</td>
                        <td className="p-3">
                          <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                            prof.role === 'admin' ? 'bg-purple-950 text-purple-400' :
                            prof.role === 'doctor' ? 'bg-cyan-950 text-cyan-400' :
                            'bg-emerald-950 text-emerald-400'
                          }`}>
                            {prof.role}
                          </span>
                        </td>
                        <td className="p-3">{prof.phone}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        )}

        {/* NATIVE KOTLIN & XML CODE INSPECTOR TAB */}
        {activeTab === 'code' && (
          <div className="w-full max-w-5xl bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-2xl space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b border-slate-800">
              <div>
                <div className="flex items-center gap-2">
                  <Code className="w-5 h-5 text-cyan-400" />
                  <h2 className="text-lg font-bold text-white">Native Android Studio Source Files</h2>
                </div>
                <p className="text-xs text-slate-400">
                  Built exclusively with Kotlin + XML layouts, Material 3, ViewBinding, and Supabase PostgREST client.
                </p>
              </div>

              <div className="flex gap-1.5 flex-wrap">
                {[
                  'schema.sql',
                  'local.properties',
                  'build.gradle.kts',
                  'SupabaseManager.kt',
                  'AndroidManifest.xml',
                  'DoctorRepository.kt',
                  'AppointmentRepository.kt',
                  'Models.kt'
                ].map((f) => (
                  <button
                    key={f}
                    onClick={() => setCodeFile(f)}
                    className={`px-3 py-1 rounded-lg text-xs font-mono transition ${
                      codeFile === f ? 'bg-cyan-600 text-white font-bold' : 'bg-slate-950 text-slate-400 hover:text-slate-200'
                    }`}
                  >
                    {f}
                  </button>
                ))}
              </div>
            </div>

            {/* Code Snippets Display */}
            <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 font-mono text-xs text-slate-300 overflow-x-auto max-h-[500px]">
              {codeFile === 'schema.sql' && (
                <pre>{`-- Supabase PostgreSQL Hospital Management Schema
CREATE TABLE profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  email TEXT UNIQUE NOT NULL,
  name TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('patient', 'doctor', 'admin')),
  phone TEXT,
  avatar_url TEXT,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE doctors (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID UNIQUE REFERENCES profiles(id) ON DELETE CASCADE,
  specialization TEXT NOT NULL,
  qualification TEXT NOT NULL,
  experience TEXT NOT NULL,
  about TEXT,
  available BOOLEAN DEFAULT true,
  image_url TEXT,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE patients (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID UNIQUE REFERENCES profiles(id) ON DELETE CASCADE,
  dob DATE,
  gender TEXT,
  blood_group TEXT,
  medical_history TEXT,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE appointments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  patient_id UUID REFERENCES patients(id) ON DELETE CASCADE,
  doctor_id UUID REFERENCES doctors(id) ON DELETE CASCADE,
  appointment_date DATE NOT NULL,
  appointment_time TEXT NOT NULL,
  reason TEXT NOT NULL,
  status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'confirmed', 'rejected', 'completed', 'cancelled')),
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE prescriptions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  appointment_id UUID REFERENCES appointments(id) ON DELETE CASCADE,
  medicine TEXT NOT NULL,
  dosage TEXT NOT NULL,
  frequency TEXT NOT NULL,
  instructions TEXT,
  created_at TIMESTAMPTZ DEFAULT now()
);`}</pre>
              )}

              {codeFile === 'AndroidManifest.xml' && (
                <pre>{`<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".MedicareApp"
        android:theme="@style/Theme.Medicare">
        
        <!-- Splash Screen Launcher -->
        <activity
            android:name=".ui.splash.SplashActivity"
            android:exported="true"
            android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity android:name=".ui.auth.WelcomeActivity" android:screenOrientation="portrait" />
        <activity android:name=".ui.auth.LoginActivity" android:screenOrientation="portrait" />
        <activity android:name=".ui.auth.RegisterActivity" android:screenOrientation="portrait" />
        <activity android:name=".ui.patient.PatientDashboardActivity" android:screenOrientation="portrait" />
        <activity android:name=".ui.patient.DoctorListActivity" android:screenOrientation="portrait" />
        <activity android:name=".ui.patient.DoctorDetailsActivity" android:screenOrientation="portrait" />
        <activity android:name=".ui.patient.BookAppointmentActivity" android:screenOrientation="portrait" />
        <activity android:name=".ui.patient.MyAppointmentsActivity" android:screenOrientation="portrait" />
        <activity android:name=".ui.patient.PrescriptionActivity" android:screenOrientation="portrait" />
        <activity android:name=".ui.doctor.DoctorDashboardActivity" android:screenOrientation="portrait" />
        <activity android:name=".ui.doctor.AddPrescriptionActivity" android:screenOrientation="portrait" />
        <activity android:name=".ui.admin.AdminDashboardActivity" android:screenOrientation="portrait" />
    </application>
</manifest>`}</pre>
              )}

              {codeFile === 'DoctorRepository.kt' && (
                <pre>{`class DoctorRepository {
    private val client = SupabaseManager.client

    suspend fun getDoctors(): Result<List<Doctor>> = withContext(Dispatchers.IO) {
        try {
            val response = client.from("doctors")
                .select(Columns.raw("*, profiles(*)"))
                .decodeList<Doctor>()
            Result.success(response)
        } catch (e: Exception) {
            Result.success(getFallbackDoctors())
        }
    }
}`}</pre>
              )}

              {codeFile === 'AppointmentRepository.kt' && (
                <pre>{`class AppointmentRepository {
    private val client = SupabaseManager.client

    suspend fun bookAppointment(
        patientId: String,
        doctorId: String,
        appointmentDate: String,
        appointmentTime: String,
        reason: String
    ): Result<Appointment> = withContext(Dispatchers.IO) {
        try {
            val record = client.from("appointments").insert(
                Appointment(
                    id = UUID.randomUUID().toString(),
                    patientId = patientId,
                    doctorId = doctorId,
                    appointmentDate = appointmentDate,
                    appointmentTime = appointmentTime,
                    reason = reason,
                    status = "pending"
                )
            ).decodeSingle<Appointment>()
            Result.success(record)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}`}</pre>
              )}

              {codeFile === 'Models.kt' && (
                <pre>{`@Serializable
enum class UserRole {
    @SerialName("patient") PATIENT,
    @SerialName("doctor") DOCTOR,
    @SerialName("admin") ADMIN
}

@Serializable
data class Profile(
    val id: String,
    val email: String,
    val name: String,
    val role: UserRole,
    val phone: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class Doctor(
    val id: String,
    val userId: String,
    val specialization: String,
    val qualification: String,
    val experience: String,
    val about: String? = null,
    val available: Boolean = true,
    val profile: Profile? = null
)`}</pre>
              )}

              {codeFile === 'local.properties' && (
                <pre>{`## Medicare Android - Supabase Credentials (local.properties)
## Kept out of GitHub via .gitignore
## Place your Supabase Project URL and public anon key here:

SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_ANON_KEY=your-supabase-public-anon-key`}</pre>
              )}

              {codeFile === 'build.gradle.kts' && (
                <pre>{`import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Load Supabase credentials from local.properties or system env to keep secrets out of GitHub
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

val supabaseUrl = localProperties.getProperty("SUPABASE_URL")
    ?: System.getenv("SUPABASE_URL")
    ?: "https://YOUR_PROJECT_REF.supabase.co"

val supabaseAnonKey = localProperties.getProperty("SUPABASE_ANON_KEY")
    ?: System.getenv("SUPABASE_ANON_KEY")
    ?: "YOUR_SUPABASE_ANON_KEY"

android {
    namespace = "com.example.medicare"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.medicare"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    }
}`}</pre>
              )}

              {codeFile === 'SupabaseManager.kt' && (
                <pre>{`package com.example.medicare.data

import android.content.Context
import com.example.medicare.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime

object SupabaseManager {
    lateinit var client: SupabaseClient
        private set

    val auth: Auth get() = client.auth
    val db: Postgrest get() = client.postgrest
    val storage: Storage get() = client.storage
    val realtime: Realtime get() = client.realtime

    fun initialize(context: Context) {
        val targetUrl = BuildConfig.SUPABASE_URL.trim()
        val targetKey = BuildConfig.SUPABASE_ANON_KEY.trim()

        client = createSupabaseClient(
            supabaseUrl = targetUrl,
            supabaseKey = targetKey
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
}`}</pre>
              )}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
