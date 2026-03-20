package com.familyalarm.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.familyalarm.data.FamilyMember
import com.familyalarm.databinding.ItemFamilyMemberBinding

class FamilyMemberAdapter(
    private val onAlarmClick: (FamilyMember) -> Unit
) : ListAdapter<FamilyMember, FamilyMemberAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FamilyMember>() {
            override fun areItemsTheSame(a: FamilyMember, b: FamilyMember) = a.uid == b.uid
            override fun areContentsTheSame(a: FamilyMember, b: FamilyMember) = a == b
        }
    }

    inner class ViewHolder(
        private val binding: ItemFamilyMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: FamilyMember) {
            binding.tvMemberName.text = member.name
            binding.tvInitial.text = member.name.firstOrNull()?.uppercase() ?: "?"
            binding.btnAlarmMember.setOnClickListener {
                onAlarmClick(member)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFamilyMemberBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
