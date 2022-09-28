// Copyright 2022 European netID Foundation (https://enid.foundation)
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.netid.mobile.sdk.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import de.netid.mobile.sdk.R
import de.netid.mobile.sdk.model.AppIdentifier


class AuthorizationAppListAdapter(
    val context: Context,
    private val items: List<AppIdentifier>,
) : BaseAdapter() {
    private val layoutInflater = LayoutInflater.from(context)
    var selectedPosition: Int = 0
    var listener: AuthorizationAppListAdapterListener? = null

    override fun getCount(): Int {
        return items.size
    }

    override fun getView(position: Int, view: View?, viewGroup: ViewGroup?): View? {
        val rowView: View?
        val viewHolder: ViewHolder
        if (view == null) {
            rowView = layoutInflater.inflate(R.layout.netid_app_cell, viewGroup, false)

            viewHolder = ViewHolder(rowView)
            rowView.tag = viewHolder

        } else {
            rowView = view
            viewHolder = rowView.tag as ViewHolder
        }

        viewHolder.itemName.text = context.resources.getString(R.string.authorization_cell_use_app_text, items[position].name)
        val resourceId: Int =
            context.resources.getIdentifier(items[position].icon, "drawable", context.opPackageName)
        viewHolder.itemImage.setImageDrawable(
            ResourcesCompat.getDrawable(
                context.resources,
                resourceId,
                null
            )
        )

        viewHolder.itemRadioButton.setOnCheckedChangeListener { _, isEnabled ->
            if (isEnabled) {
                selectedPosition = position
                listener?.onAppSelected(items[position].name)
                notifyDataSetChanged()
            }
        }
        viewHolder.itemRadioButton.isChecked = selectedPosition == position

        return rowView
    }

    override fun getItem(p0: Int): AppIdentifier {
        return items[p0]
    }

    override fun getItemId(p0: Int): Long {
        return items[p0].id.toLong()
    }

    private class ViewHolder(view: View?) {
        val itemName = view?.findViewById(R.id.netidAppCellTextView) as TextView
        val itemImage = view?.findViewById(R.id.netidAppCellImageView) as ImageView
        val itemRadioButton = view?.findViewById(R.id.netidAppCellRadioButton) as RadioButton
    }
}
