package com.example.medicare.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.medicare.R
import com.example.medicare.data.model.Appointment
import com.example.medicare.data.model.Doctor
import com.example.medicare.data.model.Patient
import com.example.medicare.databinding.ItemAppointmentBinding
import com.example.medicare.databinding.ItemDoctorAppointmentBinding
import com.example.medicare.databinding.ItemDoctorBinding
import com.example.medicare.databinding.ItemPatientBinding
import com.example.medicare.utils.AnimationUtils

class DoctorAdapter(
    private var doctors: List<Doctor>,
    private val onDoctorClick: (Doctor) -> Unit,
    private val onBookClick: (Doctor) -> Unit
) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

    fun updateData(newDoctors: List<Doctor>) {
        doctors = newDoctors
        notifyDataSetChanged()
    }

    inner class DoctorViewHolder(val binding: ItemDoctorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(doctor: Doctor) {
            val name = doctor.profile?.name?.takeIf { it.isNotBlank() } ?: doctor.doctorName
            binding.tvDoctorName.text = name
            binding.tvDoctorSpecialization.text = "${doctor.specialization} • ${doctor.department}"
            binding.tvDoctorQualification.text = "${doctor.qualification} • ${doctor.experience}"
            binding.tvDoctorInitial.text = name.replace("Dr.", "").trim().take(2).uppercase().ifBlank { "DR" }

            val feeText = doctor.consultationFee?.let { " • ₹${it.toInt()}" } ?: ""
            binding.tvDoctorRating.text = "🏥 ${doctor.hospital} • ${doctor.availableDays} (${doctor.workingHours})$feeText"

            if (doctor.available) {
                binding.tvDoctorAvailability.text = "Available"
                binding.tvDoctorAvailability.setTextColor(Color.parseColor("#10B981"))
                binding.tvDoctorAvailability.setBackgroundColor(Color.parseColor("#D1FAE5"))
            } else {
                binding.tvDoctorAvailability.text = "Unavailable"
                binding.tvDoctorAvailability.setTextColor(Color.parseColor("#EF4444"))
                binding.tvDoctorAvailability.setBackgroundColor(Color.parseColor("#FEE2E2"))
            }

            AnimationUtils.applyPressAnimation(binding.cardDoctorItem) {
                onDoctorClick(doctor)
            }

            AnimationUtils.applyPressAnimation(binding.btnBookAppointmentItem) {
                onBookClick(doctor)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val binding = ItemDoctorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DoctorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        holder.bind(doctors[position])
    }

    override fun getItemCount(): Int = doctors.size
}

class AppointmentAdapter(
    private var appointments: List<Appointment>,
    private val onCancelClick: (Appointment) -> Unit,
    private val onViewPrescriptionClick: (Appointment) -> Unit
) : RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

    fun updateData(newList: List<Appointment>) {
        appointments = newList
        notifyDataSetChanged()
    }

    inner class AppointmentViewHolder(val binding: ItemAppointmentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(apt: Appointment) {
            val docName = apt.doctor?.profile?.name ?: "Doctor"
            val spec = apt.doctor?.specialization ?: "Consultant"
            binding.tvAptDoctorName.text = docName
            binding.tvAptSpecialization.text = spec
            binding.tvAptDate.text = "📅 ${apt.appointmentDate}"
            binding.tvAptTime.text = "⏰ ${apt.appointmentTime}"
            binding.tvAptReason.text = "Reason: ${apt.reason}"

            // Status chip coloring
            when (apt.status.lowercase()) {
                "pending" -> {
                    binding.tvAptStatusChip.text = "🟡 PENDING"
                    binding.tvAptStatusChip.setTextColor(Color.parseColor("#F59E0B"))
                    binding.tvAptStatusChip.setBackgroundColor(Color.parseColor("#FEF3C7"))
                    binding.btnCancelAptItem.visibility = View.VISIBLE
                    binding.btnViewPrescriptionItem.visibility = View.GONE
                }
                "confirmed" -> {
                    binding.tvAptStatusChip.text = "🟢 CONFIRMED"
                    binding.tvAptStatusChip.setTextColor(Color.parseColor("#10B981"))
                    binding.tvAptStatusChip.setBackgroundColor(Color.parseColor("#D1FAE5"))
                    binding.btnCancelAptItem.visibility = View.VISIBLE
                    binding.btnViewPrescriptionItem.visibility = View.GONE
                }
                "completed" -> {
                    binding.tvAptStatusChip.text = "🔵 COMPLETED"
                    binding.tvAptStatusChip.setTextColor(Color.parseColor("#3B82F6"))
                    binding.tvAptStatusChip.setBackgroundColor(Color.parseColor("#DBEAFE"))
                    binding.btnCancelAptItem.visibility = View.GONE
                    binding.btnViewPrescriptionItem.visibility = View.VISIBLE
                }
                "rejected" -> {
                    binding.tvAptStatusChip.text = "🔴 REJECTED"
                    binding.tvAptStatusChip.setTextColor(Color.parseColor("#EF4444"))
                    binding.tvAptStatusChip.setBackgroundColor(Color.parseColor("#FEE2E2"))
                    binding.btnCancelAptItem.visibility = View.GONE
                    binding.btnViewPrescriptionItem.visibility = View.GONE
                }
                else -> {
                    binding.tvAptStatusChip.text = "⚪ CANCELLED"
                    binding.tvAptStatusChip.setTextColor(Color.parseColor("#6B7280"))
                    binding.tvAptStatusChip.setBackgroundColor(Color.parseColor("#F3F4F6"))
                    binding.btnCancelAptItem.visibility = View.GONE
                    binding.btnViewPrescriptionItem.visibility = View.GONE
                }
            }

            AnimationUtils.applyPressAnimation(binding.btnCancelAptItem) {
                onCancelClick(apt)
            }

            AnimationUtils.applyPressAnimation(binding.btnViewPrescriptionItem) {
                onViewPrescriptionClick(apt)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val binding = ItemAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppointmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(appointments[position])
    }

    override fun getItemCount(): Int = appointments.size
}

class DoctorAppointmentAdapter(
    private var appointments: List<Appointment>,
    private val onAcceptClick: (Appointment) -> Unit,
    private val onRejectClick: (Appointment) -> Unit,
    private val onCompleteClick: (Appointment) -> Unit,
    private val onAddPrescriptionClick: (Appointment) -> Unit
) : RecyclerView.Adapter<DoctorAppointmentAdapter.DoctorAppointmentViewHolder>() {

    fun updateData(newList: List<Appointment>) {
        appointments = newList
        notifyDataSetChanged()
    }

    inner class DoctorAppointmentViewHolder(val binding: ItemDoctorAppointmentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(apt: Appointment) {
            val patientName = apt.patient?.profile?.name ?: "Patient"
            val gender = apt.patient?.gender ?: "Unknown"
            val blood = apt.patient?.bloodGroup ?: "N/A"

            binding.tvDocAptPatientName.text = patientName
            binding.tvDocPatientInitial.text = patientName.take(1).uppercase()
            binding.tvDocAptPatientDetails.text = "$gender • Blood: $blood"
            binding.tvDocAptDateTime.text = "📅 ${apt.appointmentDate} • ⏰ ${apt.appointmentTime}"
            binding.tvDocAptReason.text = "Reason: ${apt.reason}"

            // Status chip & actions
            when (apt.status.lowercase()) {
                "pending" -> {
                    binding.tvDocAptStatusChip.text = "PENDING"
                    binding.tvDocAptStatusChip.setTextColor(Color.parseColor("#F59E0B"))
                    binding.tvDocAptStatusChip.setBackgroundColor(Color.parseColor("#FEF3C7"))
                    binding.btnDocRejectApt.visibility = View.VISIBLE
                    binding.btnDocAcceptApt.visibility = View.VISIBLE
                    binding.btnDocCompleteApt.visibility = View.GONE
                    binding.btnDocAddPrescription.visibility = View.GONE
                }
                "confirmed" -> {
                    binding.tvDocAptStatusChip.text = "CONFIRMED"
                    binding.tvDocAptStatusChip.setTextColor(Color.parseColor("#10B981"))
                    binding.tvDocAptStatusChip.setBackgroundColor(Color.parseColor("#D1FAE5"))
                    binding.btnDocRejectApt.visibility = View.GONE
                    binding.btnDocAcceptApt.visibility = View.GONE
                    binding.btnDocCompleteApt.visibility = View.VISIBLE
                    binding.btnDocAddPrescription.visibility = View.GONE
                }
                "completed" -> {
                    binding.tvDocAptStatusChip.text = "COMPLETED"
                    binding.tvDocAptStatusChip.setTextColor(Color.parseColor("#3B82F6"))
                    binding.tvDocAptStatusChip.setBackgroundColor(Color.parseColor("#DBEAFE"))
                    binding.btnDocRejectApt.visibility = View.GONE
                    binding.btnDocAcceptApt.visibility = View.GONE
                    binding.btnDocCompleteApt.visibility = View.GONE
                    binding.btnDocAddPrescription.visibility = View.VISIBLE
                }
                else -> {
                    binding.tvDocAptStatusChip.text = apt.status.uppercase()
                    binding.tvDocAptStatusChip.setTextColor(Color.parseColor("#6B7280"))
                    binding.tvDocAptStatusChip.setBackgroundColor(Color.parseColor("#F3F4F6"))
                    binding.btnDocRejectApt.visibility = View.GONE
                    binding.btnDocAcceptApt.visibility = View.GONE
                    binding.btnDocCompleteApt.visibility = View.GONE
                    binding.btnDocAddPrescription.visibility = View.GONE
                }
            }

            AnimationUtils.applyPressAnimation(binding.btnDocAcceptApt) { onAcceptClick(apt) }
            AnimationUtils.applyPressAnimation(binding.btnDocRejectApt) { onRejectClick(apt) }
            AnimationUtils.applyPressAnimation(binding.btnDocCompleteApt) { onCompleteClick(apt) }
            AnimationUtils.applyPressAnimation(binding.btnDocAddPrescription) { onAddPrescriptionClick(apt) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorAppointmentViewHolder {
        val binding = ItemDoctorAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DoctorAppointmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DoctorAppointmentViewHolder, position: Int) {
        holder.bind(appointments[position])
    }

    override fun getItemCount(): Int = appointments.size
}

class PatientAdapter(
    private var patients: List<Patient>,
    private val onPatientClick: (Patient) -> Unit
) : RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {

    fun updateData(newList: List<Patient>) {
        patients = newList
        notifyDataSetChanged()
    }

    inner class PatientViewHolder(val binding: ItemPatientBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(patient: Patient) {
            val name = patient.profile?.name ?: "Patient"
            binding.tvItemPatientName.text = name
            binding.tvItemPatientInitial.text = name.take(1).uppercase()
            binding.tvItemPatientContact.text = "${patient.profile?.phone ?: "No Phone"}"
            binding.tvItemPatientBlood.text = patient.bloodGroup ?: "O+"

            binding.root.setOnClickListener { onPatientClick(patient) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val binding = ItemPatientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PatientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(patients[position])
    }

    override fun getItemCount(): Int = patients.size
}

class AdminAppointmentAdapter(
    private var appointments: List<Appointment>,
    private val onUpdateStatusClick: (Appointment) -> Unit
) : RecyclerView.Adapter<AdminAppointmentAdapter.AdminAppointmentViewHolder>() {

    fun updateData(newList: List<Appointment>) {
        appointments = newList
        notifyDataSetChanged()
    }

    inner class AdminAppointmentViewHolder(val binding: com.example.medicare.databinding.ItemAdminAppointmentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(apt: Appointment) {
            val patientName = apt.patient?.profile?.name ?: "Patient"
            val docName = apt.doctor?.profile?.name ?: "Doctor"

            binding.tvAdminAptPatientName.text = "Patient: $patientName"
            binding.tvAdminAptDoctorName.text = "Doctor: $docName (${apt.doctor?.specialization ?: "General"})"
            binding.tvAdminAptDateTime.text = "📅 ${apt.appointmentDate} • ⏰ ${apt.appointmentTime}"
            binding.tvAdminAptReason.text = "Reason: ${apt.reason}"

            when (apt.status.lowercase()) {
                "pending" -> {
                    binding.tvAdminAptStatusChip.text = "PENDING"
                    binding.tvAdminAptStatusChip.setTextColor(Color.parseColor("#F59E0B"))
                    binding.tvAdminAptStatusChip.setBackgroundColor(Color.parseColor("#FEF3C7"))
                }
                "confirmed" -> {
                    binding.tvAdminAptStatusChip.text = "CONFIRMED"
                    binding.tvAdminAptStatusChip.setTextColor(Color.parseColor("#10B981"))
                    binding.tvAdminAptStatusChip.setBackgroundColor(Color.parseColor("#D1FAE5"))
                }
                "completed" -> {
                    binding.tvAdminAptStatusChip.text = "COMPLETED"
                    binding.tvAdminAptStatusChip.setTextColor(Color.parseColor("#3B82F6"))
                    binding.tvAdminAptStatusChip.setBackgroundColor(Color.parseColor("#DBEAFE"))
                }
                "rejected" -> {
                    binding.tvAdminAptStatusChip.text = "REJECTED"
                    binding.tvAdminAptStatusChip.setTextColor(Color.parseColor("#EF4444"))
                    binding.tvAdminAptStatusChip.setBackgroundColor(Color.parseColor("#FEE2E2"))
                }
                else -> {
                    binding.tvAdminAptStatusChip.text = "CANCELLED"
                    binding.tvAdminAptStatusChip.setTextColor(Color.parseColor("#6B7280"))
                    binding.tvAdminAptStatusChip.setBackgroundColor(Color.parseColor("#F3F4F6"))
                }
            }

            binding.btnAdminAptUpdateStatus.setOnClickListener {
                onUpdateStatusClick(apt)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminAppointmentViewHolder {
        val binding = com.example.medicare.databinding.ItemAdminAppointmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AdminAppointmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminAppointmentViewHolder, position: Int) {
        holder.bind(appointments[position])
    }

    override fun getItemCount(): Int = appointments.size
}
