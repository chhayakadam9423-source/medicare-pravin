package com.example.medicare.ui.patient

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.databinding.ActivityDoctorDetailsBinding
import com.example.medicare.utils.AnimationUtils

class DoctorDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val docId = intent.getStringExtra("DOCTOR_ID") ?: ""
        val docName = intent.getStringExtra("DOCTOR_NAME") ?: "Dr. Specialist"
        val specialty = intent.getStringExtra("DOCTOR_SPECIALIZATION") ?: "General Consultant"
        val qual = intent.getStringExtra("DOCTOR_QUALIFICATION") ?: "MBBS, MD"
        val exp = intent.getStringExtra("DOCTOR_EXPERIENCE") ?: "10+ Years"
        val hospital = intent.getStringExtra("DOCTOR_HOSPITAL") ?: "Medicare Central Hospital"
        val department = intent.getStringExtra("DOCTOR_DEPARTMENT") ?: "General Department"
        val fee = intent.getDoubleExtra("DOCTOR_FEE", 500.0)
        val days = intent.getStringExtra("DOCTOR_DAYS") ?: "Mon, Tue, Wed, Thu, Fri"
        val hours = intent.getStringExtra("DOCTOR_HOURS") ?: "09:00 AM - 05:00 PM"
        val about = intent.getStringExtra("DOCTOR_ABOUT") ?: "Experienced medical professional dedicated to providing compassionate, high quality patient healthcare at $hospital ($department)."

        binding.toolbarDoctorDetails.setNavigationOnClickListener {
            finish()
        }

        binding.tvDocDetailName.text = docName
        binding.tvDocDetailInitial.text = docName.removePrefix("Dr. ").trim().take(2).uppercase().ifBlank { "DR" }
        binding.tvDocDetailSpecialty.text = "$specialty • $department"
        binding.tvDocDetailQualification.text = "$qual • $hospital"
        binding.tvDocDetailExp.text = exp
        binding.tvDocDetailAbout.text = "$about\n\nConsultation Fee: ₹${fee.toInt()}\nAvailable Days: $days\nHours: $hours"

        AnimationUtils.applyPressAnimation(binding.btnBookAppointmentProceed) {
            val intent = Intent(this, BookAppointmentActivity::class.java).apply {
                putExtra("DOCTOR_ID", docId)
                putExtra("DOCTOR_NAME", docName)
                putExtra("DOCTOR_SPECIALIZATION", specialty)
                putExtra("DOCTOR_FEE", fee)
            }
            startActivity(intent)
        }
    }
}
