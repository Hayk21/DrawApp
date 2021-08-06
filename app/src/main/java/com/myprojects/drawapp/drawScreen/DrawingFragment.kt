package com.myprojects.drawapp.drawScreen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.myprojects.drawapp.R
import com.myprojects.drawapp.customview.DrawingView
import com.myprojects.drawapp.databinding.DrawingFragmentBinding
import com.myprojects.drawapp.extension.convertToBitmap
import com.myprojects.drawapp.model.BrushSize


class DrawingFragment : Fragment() {

    private val viewModel: DrawingViewModel by viewModels()
    private lateinit var binding: DrawingFragmentBinding

    private var mainColors: Bitmap? = null
    private var colors: Bitmap? = null
    private var screenWidth: Int? = null
    private lateinit var requestMultiplePermissions: ActivityResultLauncher<Array<String>>

    private var pickerResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val bitmap = MediaStore.Images.Media.getBitmap(
                    requireActivity().getContentResolver(),
                    result.data?.data
                )
                binding.drawingView.backgroundImage = bitmap
                binding.backImage.setImageBitmap(bitmap)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        requestMultiplePermissions = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.values.forEach {
                if (!it) {
                    return@registerForActivityResult
                } else {
                    viewModel.saveImage(binding.drawingView.getImageBitmap(), requireContext())
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DrawingFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val display =
            requireContext().display
        val size = Point()
        display?.getSize(size)
        screenWidth = size.x - resources.getDimensionPixelSize(R.dimen.d5) * 2

        initUI()
        setListeners()

        viewModel.savedStatus.observe(viewLifecycleOwner, Observer {
            it?.getValue()?.let {
                Toast.makeText(
                    requireContext(),
                    getString(if (it) R.string.success_saved else R.string.failed_saved),
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.title) {
            getString(R.string.save_image) -> {
                if (checkAndRequestPermissions()) {
                    viewModel.saveImage(binding.drawingView.getImageBitmap(), requireContext())
                }
            }
            getString(R.string.choose_image) -> {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                pickerResultLauncher.launch(Intent.createChooser(intent, "Select Picture"))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initUI() {
        val mainGradient = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(
                resources.getColor(R.color.color_picker_1, null),
                resources.getColor(R.color.color_picker_2, null),
                resources.getColor(R.color.color_picker_3, null),
                resources.getColor(R.color.color_picker_4, null),
                resources.getColor(R.color.color_picker_5, null),
                resources.getColor(R.color.color_picker_6, null)
            )
        )

        val blackWhiteGradient = getPickerGradient(resources.getColor(R.color.color_picker_1, null))

        binding.colorProgressImage.setImageDrawable(mainGradient)

        binding.blackWhiteProgress.setImageDrawable(blackWhiteGradient)

        mainColors = mainGradient.convertToBitmap(
            screenWidth!!,
            resources.getDimensionPixelSize(R.dimen.d20)
        )
        colors = blackWhiteGradient.convertToBitmap(
            screenWidth!!,
            resources.getDimensionPixelSize(R.dimen.d20)
        )
        binding.colorPicker.max = mainColors!!.width - 1
        binding.blackWhitePicker
            .apply {
                max = colors!!.width - 1
                progress = max / 2
            }

        binding.drawingView.setColor(
            colors!!.getPixel(
                binding.blackWhitePicker.progress, 0
            )
        )

        binding.bottomToolbar.selectedItemId = R.id.middle_brush
    }

    private fun getPickerGradient(color: Int): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(
                resources.getColor(R.color.black, null),
                color,
                resources.getColor(R.color.white, null),
            )
        )
    }

    private fun setListeners() {
        binding.bottomToolbar.setOnItemSelectedListener {
            if (it.itemId == R.id.eraser) {
                binding.drawingView.setErasingMode(true)
            } else {
                binding.drawingView.setErasingMode(false)
                binding.drawingView.setBrushSize(getSizeById(it.itemId))
            }
            true
        }

        binding.colorPicker.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val pixel = mainColors!!.getPixel(progress, 0)
                colors = getPickerGradient(pixel).convertToBitmap(
                    screenWidth!!,
                    resources.getDimensionPixelSize(R.dimen.d20)
                )
                binding.blackWhiteProgress.setImageBitmap(colors)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                binding.drawingView.setColor(
                    colors!!.getPixel(
                        binding.blackWhitePicker.progress,
                        0
                    )
                )
            }
        })

        binding.blackWhitePicker.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                binding.drawingView.setColor(colors!!.getPixel(seekBar.progress, 0))
            }
        })

        binding.drawingView.setOnHistoryListener(object : DrawingView.DrawHistoryListener {
            override fun historyChanged(backAvailable: Boolean, nextAvailable: Boolean) {
                if (binding.backHistory.visibility == View.INVISIBLE) {
                    binding.backHistory.visibility = View.VISIBLE
                    binding.nextHistory.visibility = View.VISIBLE
                }
                binding.backHistory.isEnabled = backAvailable
                binding.nextHistory.isEnabled = nextAvailable
            }

            override fun showHistoryItems(show: Boolean) {
                binding.backHistory.visibility = if (show) View.VISIBLE else View.INVISIBLE
                binding.nextHistory.visibility = if (show) View.VISIBLE else View.INVISIBLE
            }
        })

        binding.backHistory.setOnClickListener { binding.drawingView.backHistory() }
        binding.nextHistory.setOnClickListener { binding.drawingView.nextHistory() }
    }

    private fun getSizeById(id: Int): BrushSize? {
        return when (id) {
            R.id.small_brush -> BrushSize.SMALL
            R.id.middle_brush -> BrushSize.MIDDLE
            R.id.big_brush -> BrushSize.BIG
            else -> null
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val writePermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val readPermission =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        if (writePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (readPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (listPermissionsNeeded.isNotEmpty()) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
            return false
        }
        return true
    }
}