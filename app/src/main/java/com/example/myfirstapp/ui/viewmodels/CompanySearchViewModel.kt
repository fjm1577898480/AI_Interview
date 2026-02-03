package com.example.myfirstapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfirstapp.data.CompanyData
import com.example.myfirstapp.data.model.Company
import com.example.myfirstapp.data.model.CompanyListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

class CompanySearchViewModel : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedIndustry = MutableStateFlow<String?>(null)
    val selectedIndustry: StateFlow<String?> = _selectedIndustry.asStateFlow()

    // 原始数据
    private val allCompanyData = CompanyData.getCompanyData()

    // UI 数据流
    val uiList: StateFlow<List<CompanyListItem>> = combine(
        _searchQuery,
        _selectedIndustry
    ) { query, industry ->
        Triple(query, industry, allCompanyData)
    }.map { (query, industry, data) ->
        // 在后台线程进行数据处理
        if (query.isEmpty() && industry == null) {
            emptyList()
        } else {
            generateUiList(query, industry, data)
        }
    }
    .flowOn(Dispatchers.Default) // 确保计算在后台线程
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onIndustrySelected(industry: String?) {
        _selectedIndustry.value = industry
    }

    private fun generateUiList(
        query: String,
        industry: String?,
        data: Map<String, List<Company>>
    ): List<CompanyListItem> {
        val result = mutableListOf<CompanyListItem>()

        if (industry != null) {
            // 单个行业模式
            val companies = data[industry] ?: emptyList()
            val filtered = if (query.isNotEmpty()) {
                companies.filter { it.name.contains(query, ignoreCase = true) }
            } else {
                companies
            }

            if (filtered.isNotEmpty()) {
                // 按拼音首字母分组并添加 Header
                val grouped = filtered.groupBy { it.pinyinFirstLetter }
                // 确保按字母顺序排列
                grouped.toSortedMap().forEach { (letter, list) ->
                    result.add(CompanyListItem.Header(letter.toString(), "header_${industry}_$letter"))
                    list.forEach { company ->
                        result.add(CompanyListItem.Item(company, "${industry}_${company.name}"))
                    }
                }
            }
        } else {
            // 全局搜索模式
            data.forEach { (ind, companies) ->
                val filtered = companies.filter { it.name.contains(query, ignoreCase = true) }
                if (filtered.isNotEmpty()) {
                    result.add(CompanyListItem.Header(ind, "header_$ind"))
                    filtered.forEach { company ->
                        result.add(CompanyListItem.Item(company, "${ind}_${company.name}"))
                    }
                }
            }
        }
        return result
    }
}