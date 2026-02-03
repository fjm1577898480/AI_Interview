package com.example.myfirstapp.data.model

// 数据模型：包含拼音首字母用于排序
data class Company(val name: String, val pinyinFirstLetter: Char)

// UI 列表项模型，用于扁平化列表以优化性能
sealed interface CompanyListItem {
    data class Header(val title: String, val key: String) : CompanyListItem
    data class Item(val company: Company, val key: String) : CompanyListItem
}