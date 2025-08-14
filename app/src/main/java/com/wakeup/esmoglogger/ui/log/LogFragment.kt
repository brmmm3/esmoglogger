package com.wakeup.esmoglogger.ui.log

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wakeup.esmoglogger.R

class LogFragment : Fragment() {
    private var recyclerView: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_log, container, false)
        recyclerView = view.findViewById<RecyclerView?>(R.id.logView)
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
        view?.findViewById<RecyclerView?>(R.id.nav_view)?.layoutManager = null
    }
}