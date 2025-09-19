package com.wakeup.esmoglogger.ui.cloud

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wakeup.esmoglogger.R

class CloudFragment : Fragment() {
    private var recyclerView: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_cloud, container, false)
        recyclerView = view.findViewById<RecyclerView?>(R.id.cloud_view)
        recyclerView?.layoutManager = LinearLayoutManager(this.context)
        val adapter = LogAdapter(emptyList())
        recyclerView?.adapter = adapter
        SharedLogData.data.observe(viewLifecycleOwner) { logs ->
            recyclerView?.adapter = LogAdapter(logs)
            recyclerView?.scrollToPosition(logs.size - 1) // Auto-scroll to latest log
        }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        view?.findViewById<RecyclerView?>(R.id.custom_bottom_nav)?.layoutManager = null
    }
}