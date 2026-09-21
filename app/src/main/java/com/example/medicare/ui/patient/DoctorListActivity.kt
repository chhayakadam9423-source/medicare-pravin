package com.example.medicare.ui.patient

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.medicare.R
import com.example.medicare.adapter.DoctorAdapter
import com.example.medicare.data.model.Doctor
import com.example.medicare.data.repository.DoctorRepository
import com.example.medicare.databinding.ActivityDoctorListBinding
import kotlinx.coroutines.launch

class DoctorListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorListBinding
    private val doctorRepository = DoctorRepository()
    private lateinit var doctorAdapter: DoctorAdapter
    private var allDoctors: List<Doctor> = emptyList()
    private var currentSpecialty: String = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val initialFilter = intent.getStringExtra("SPECIALTY_FILTER")
        if (!initialFilter.isNullOrEmpty()) {
            currentSpecialty = initialFilter
        }

        setupUI()
        setupListeners()
        loadDoctors()
    }

    private fun setupUI() {
        binding.toolbarDoctorList.setNavigationOnClickListener {
            finish()
        }

        doctorAdapter = DoctorAdapter(
            doctors = emptyList(),
            onDoctorClick = { doctor ->
                val intent = Intent(this, DoctorDetailsActivity::class.java).apply {
                    putExtra("DOCTOR_ID", doctor.id)
                    putExtra("DOCTOR_NAME", doctor.profile?.name)
                    putExtra("DOCTOR_SPECIALIZATION", doctor.specialization)
                    putExtra("DOCTOR_QUALIFICATION", doctor.qualification)
                    putExtra("DOCTOR_EXPERIENCE", doctor.experience)
                    putExtra("DOCTOR_ABOUT", doctor.about)
                }
                startActivity(intent)
            },
            onBookClick = { doctor ->
                val intent = Intent(this, BookAppointmentActivity::class.java).apply {
                    putExtra("DOCTOR_ID", doctor.id)
                    putExtra("DOCTOR_NAME", doctor.profile?.name)
                    putExtra("DOCTOR_SPECIALIZATION", doctor.specialization)
                }
                startActivity(intent)
            }
        )

        binding.rvAllDoctors.apply {
            layoutManager = LinearLayoutManager(this@DoctorListActivity)
            adapter = doctorAdapter
        }

        // Set initial chip checked state if passed
        when (currentSpecialty.lowercase()) {
            "cardiologist" -> binding.chipCardio.isChecked = true
            "neurologist" -> binding.chipNeuro.isChecked = true
            "pediatrician" -> binding.chipPediatric.isChecked = true
            "orthopedic" -> binding.chipOrtho.isChecked = true
            else -> binding.chipAll.isChecked = true
        }
    }

    private fun setupListeners() {
        // Search text watcher
        binding.etDoctorSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterDoctors()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Filter chips
        binding.chipGroupSpecialties.setOnCheckedStateChangeListener { _, checkedIds ->
            currentSpecialty = if (checkedIds.contains(R.id.chipCardio)) {
                "Cardiologist"
            } else if (checkedIds.contains(R.id.chipNeuro)) {
                "Neurologist"
            } else if (checkedIds.contains(R.id.chipPediatric)) {
                "Pediatrician"
            } else if (checkedIds.contains(R.id.chipOrtho)) {
                "Orthopedic"
            } else {
                "All"
            }
            filterDoctors()
        }
    }

    private fun loadDoctors() {
        binding.pbDoctorList.visibility = View.VISIBLE
        binding.llEmptyDoctors.visibility = View.GONE

        lifecycleScope.launch {
            val result = doctorRepository.getDoctors()
            binding.pbDoctorList.visibility = View.GONE

            result.fold(
                onSuccess = { list ->
                    allDoctors = list
                    filterDoctors()
                },
                onFailure = {
                    filterDoctors()
                }
            )
        }
    }

    private fun filterDoctors() {
        val query = binding.etDoctorSearch.text?.toString()?.trim().orEmpty().lowercase()

        val filtered = allDoctors.filter { doc ->
            val nameMatches = (doc.profile?.name ?: "").lowercase().contains(query)
            val specialtyMatches = doc.specialization.lowercase().contains(query)
            val queryMatch = query.isEmpty() || nameMatches || specialtyMatches

            val categoryMatch = if (currentSpecialty == "All") {
                true
            } else {
                doc.specialization.equals(currentSpecialty, ignoreCase = true)
            }

            queryMatch && categoryMatch
        }

        doctorAdapter.updateData(filtered)

        if (filtered.isEmpty()) {
            binding.llEmptyDoctors.visibility = View.VISIBLE
            binding.rvAllDoctors.visibility = View.GONE
        } else {
            binding.llEmptyDoctors.visibility = View.GONE
            binding.rvAllDoctors.visibility = View.VISIBLE
        }
    }
}
