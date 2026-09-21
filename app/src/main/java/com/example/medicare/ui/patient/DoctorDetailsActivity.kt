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
        val about = intent.getStringExtra("DOCTOR_ABOUT") ?: "Experienced medical professional dedicated to providing compassionate, high quality patient healthcare."

        binding.toolbarDoctorDetails.setNavigationOnClickListener {
            finish()
        }

        binding.tvDocDetailName.text = docName
        binding.tvDocDetailInitial.text = docName.take(2).uppercase()
        binding.tvDocDetailSpecialty.text = specialty
        binding.tvDocDetailQualification.text = qual
        binding.tvDocDetailExp.text = exp
        binding.tvDocDetailAbout.text = about

        AnimationUtils.applyPressAnimation(binding.btnBookAppointmentProceed) {
            val intent = Intent(this, BookAppointmentActivity::class.java).apply {
                putExtra("DOCTOR_ID", docId)
                putExtra("DOCTOR_NAME", docName)
                putExtra("DOCTOR_SPECIALIZATION", specialty)
            }
            startActivity(intent)
        }
    }
}
