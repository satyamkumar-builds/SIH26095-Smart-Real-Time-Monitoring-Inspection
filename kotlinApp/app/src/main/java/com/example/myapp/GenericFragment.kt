package com.example.myapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class GenericFragment : Fragment() {

    companion object {
        private const val ARG_TITLE = "arg_title"

        fun newInstance(title: String): GenericFragment {
            val fragment = GenericFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_placeholder, container, false)
        val title = arguments?.getString(ARG_TITLE) ?: "Screen"
        view.findViewById<TextView>(R.id.tv_placeholder_title).text = title
        return view
    }
}
