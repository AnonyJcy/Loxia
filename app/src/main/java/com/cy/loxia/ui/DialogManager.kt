package com.cy.loxia.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import com.cy.loxia.DressItem
import com.cy.loxia.R
import com.cy.loxia.Wardrobe
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class DialogManager(
    context: Context,
    private val layoutInflater: LayoutInflater
) {
    // 保留 Activity context 用于 Dialog
    private val context: Context = context
    private val costDialogMessages = arrayOf(
        "(๑•̀ㅂ•́)و✧ 确定要揭开你的钱包秘密吗？",
        "(｡･ω･｡)ﾉ♡ 要查看你的裙裙总花费嘛？不许心疼哦",
        "(≧▽≦)ﾉ 前方高能预警！要查看你的快乐账单吗？",
        "(๑´•. • `๑) 真的要看看你为小裙子花了多少吗？",
        "(✧ω✧) 要解锁你的Lolita消费记录吗？准备好了吗？",
        "(｡･ω･｡) 确定要面对你的裙裙账单暴击吗？",
        "(≧∇≦)ﾉ 点击确定，就能看见你的衣柜战绩啦！",
        "(๑•́ω•̀๑) 要查看你的甜系小裙子花费总额吗？",
        "(✧∇✧) 确认要解锁你的吃土总金额吗？",
        "(｡･ω･｡)ﾉ♡ 确定要查看你为美丽付出的总花费吗？"
    )

    @JvmOverloads
    fun showTotalCostDialog(totalCost: Double, onConfirm: DialogCallbacks.OnConfirm? = null) {
        val message = costDialogMessages[(costDialogMessages.indices).random()]
        MaterialAlertDialogBuilder(context, R.style.RoundedAlertDialog)
            .setTitle("查看总花费")
            .setMessage(message)
            .setPositiveButton("确定") { _, _ -> onConfirm?.onConfirm() }
            .setNegativeButton("取消", null)
            .show()
    }

    @JvmOverloads
    fun showChannelPickerDialog(currentChannel: String, onSelect: DialogCallbacks.OnSelect<String>? = null) {
        val channels = arrayOf("淘宝", "拼多多", "闲鱼", "其他")
        val currentIndex = channels.indexOf(currentChannel).coerceAtLeast(0)

        MaterialAlertDialogBuilder(context, R.style.RoundedAlertDialog)
            .setTitle("选择渠道")
            .setSingleChoiceItems(channels, currentIndex) { dialog, which ->
                onSelect?.onSelect(channels[which])
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    @JvmOverloads
    fun showDatePickerDialog(onDateSelected: DialogCallbacks.OnSelect<String>? = null) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val date = String.format("%04d-%02d-%02d", year, month + 1, day)
                onDateSelected?.onSelect(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    @JvmOverloads
    fun showTimePickerDialog(hour: Int, minute: Int, onTimeSelected: ((Int, Int) -> Unit)? = null) {
        TimePickerDialog(
            context,
            { _, h, m -> onTimeSelected?.invoke(h, m) },
            hour,
            minute,
            true
        ).show()
    }

    @JvmOverloads
    fun showAddWardrobeDialog(onAdd: DialogCallbacks.OnSelect<String>? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_wardrobe, null)
        val etName = dialogView.findViewById<EditText>(R.id.etWardrobeName)

        MaterialAlertDialogBuilder(context, R.style.RoundedAlertDialog)
            .setTitle("新建衣柜")
            .setView(dialogView)
            .setPositiveButton("创建") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    onAdd?.onSelect(name)
                } else {
                    Toast.makeText(context, "请输入衣柜名称", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    @JvmOverloads
    fun showWardrobeContextMenu(wardrobe: Wardrobe, onAction: DialogCallbacks.OnAction? = null) {
        val options = arrayOf("删除", "上移", "下移")
        MaterialAlertDialogBuilder(context, R.style.RoundedAlertDialog)
            .setTitle(wardrobe.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showDeleteWardrobeDialog(wardrobe, object : DialogCallbacks.OnConfirm {
                        override fun onConfirm() { onAction?.onAction("delete") }
                    })
                    1 -> onAction?.onAction("move_up")
                    2 -> onAction?.onAction("move_down")
                }
            }
            .show()
    }

    @JvmOverloads
    fun showDeleteWardrobeDialog(wardrobe: Wardrobe, onDelete: DialogCallbacks.OnConfirm? = null) {
        MaterialAlertDialogBuilder(context, R.style.RoundedAlertDialog)
            .setTitle("删除衣柜")
            .setMessage("确定要删除「${wardrobe.name}」吗？\n衣柜中的所有裙子也会被删除。")
            .setPositiveButton("删除") { _, _ -> onDelete?.onConfirm() }
            .setNegativeButton("取消", null)
            .show()
    }

    @JvmOverloads
    fun showDressItemContextMenu(item: DressItem, onAction: DialogCallbacks.OnAction? = null) {
        val options = if (item.isPinned()) {
            arrayOf("取消置顶", "移到顶部", "移到底部", "删除")
        } else {
            arrayOf("置顶", "移到顶部", "移到底部", "删除")
        }

        MaterialAlertDialogBuilder(context, R.style.RoundedAlertDialog)
            .setTitle(item.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onAction?.onAction(if (item.isPinned()) "unpin" else "pin")
                    1 -> onAction?.onAction("move_to_top")
                    2 -> onAction?.onAction("move_to_bottom")
                    3 -> onAction?.onAction("delete")
                }
            }
            .show()
    }

    @JvmOverloads
    fun showWardrobePickerForImport(wardrobes: List<Wardrobe>, onSelect: DialogCallbacks.OnSelect<Wardrobe>? = null) {
        if (wardrobes.isEmpty()) {
            Toast.makeText(context, "请先创建一个衣柜", Toast.LENGTH_SHORT).show()
            return
        }

        val names = wardrobes.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(context, R.style.RoundedAlertDialog)
            .setTitle("选择目标衣柜")
            .setItems(names) { _, which -> onSelect?.onSelect(wardrobes[which]) }
            .show()
    }
}
