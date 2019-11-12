package com.rakuishi.chipsedittext

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Outline
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import kotlin.math.max

class ChipsEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RelativeLayout(context, attrs, defStyle) {

    interface Callback {
        fun onTextChanged(text: String)
    }

    private val scrollView: HorizontalScrollView
    private val container: LinearLayout
    private val clearImageView: ImageView
    private val editText: EditText =
        View.inflate(context, R.layout.view_chips_edit_text_edit_text, null) as EditText
    var callback: Callback? = null
    private val chips: ArrayList<String> = arrayListOf()
    private val placeholder: String = context.getString(R.string.placeholder)

    init {
        // view
        View.inflate(context, R.layout.view_chips_edit_text, this)
        clipToOutline(this)

        scrollView = findViewById(R.id.scrollView)
        container = findViewById(R.id.container)
        clearImageView = findViewById(R.id.clearImageView)

        clearImageView.setOnClickListener {
            clear()
        }

        setupEditText()
        updateEditTextPlaceholder()
        container.addView(editText)
        autoFitEditText()
    }

    private fun updateClearImageViewVisibility() {
        clearImageView.visibility = if (hasChipsOrText()) View.VISIBLE else View.GONE
    }

    private fun updateEditTextPlaceholder() {
        editText.hint = if (container.childCount > 1) "" else placeholder
    }

    private fun requestEditTextFocus() {
        scrollView.scrollTo(scrollView.right, 0)
        showKeyboard()
    }

    private fun clipToOutline(view: View) {
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val dp16 = dpToPx(16)
                outline.setRoundRect(0, 0, view.width, view.height, dp16.toFloat())
            }
        }
        view.clipToOutline = true
    }


    // region chips

    private fun hasChipsOrText() = chips.isNotEmpty() || editText.text.isNotEmpty()

    fun addChip(text: String) {
        chips.add(text)

        val chip = View.inflate(context, R.layout.view_chips_edit_text_chip, null)
        chip.findViewById<TextView>(R.id.textView).text = text
        chip.setOnClickListener { requestEditTextFocus() }
        container.addView(chip, container.childCount - 1) // add a chip before EditText

        editText.setText("")
        updateEditTextPlaceholder()
        autoFitEditText()
    }

    private fun removeLastChipIfPossible() {
        if (chips.isEmpty()) return
        chips.removeAt(chips.lastIndex)
        container.removeViewAt(container.childCount - 2) // remove a chip it is in front of EditText
        updateEditTextPlaceholder()
        autoFitEditText()
        updateClearImageViewVisibility()
    }

    private fun clear() {
        chips.clear()
        while (container.childCount > 1) {
            container.removeViewAt(0)
        }
        editText.setText("")
        updateEditTextPlaceholder()
        autoFitEditText()
    }

    // endregion

    // region EditText

    private fun setupEditText() {
        editText.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dpToPx(32)
        )

        // handle back space
        editText.setOnKeyListener { _, _, event ->
            // backspace
            if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
                // remove last chip
                if (editText.selectionStart == 0) {
                    removeLastChipIfPossible()
                }
            }
            false
        }

        // text changed
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                /* do nothing */
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                updateClearImageViewVisibility()
                scrollView.scrollTo(scrollView.right, 0)
                callback?.onTextChanged(s.toString())
            }

            override fun afterTextChanged(s: Editable) {
                /* do nothing */
            }
        })
    }

    private fun autoFitEditText() {
        // min width of edit text = 48 dp
        editText.minWidth = dpToPx(48)

        // listen to change in the tree
        editText.viewTreeObserver
            .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

                @SuppressLint("ObsoleteSdkInt")
                override fun onGlobalLayout() {
                    // get right of recycler and left of edit text
                    val right = scrollView.right
                    val left = editText.left

                    // edit text will fill the space
                    val minWidth = right - left - dpToPx(16)
                    editText.minWidth = max(dpToPx(48), minWidth)

                    // request focus
                    editText.requestFocus()

                    // remove the listener:
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        editText.viewTreeObserver.removeGlobalOnLayoutListener(this)
                    } else {
                        editText.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }

            })
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun showKeyboard() {
        editText.requestFocus()
        val inputManager = editText.context.getSystemService(
            Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        inputManager.showSoftInput(editText, 0)
    }

    // endregion
}
