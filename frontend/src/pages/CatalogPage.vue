<template>
  <div class="catalog-page">
    <div class="page-header">
      <h2 class="page-title">数据资产目录</h2>
      <div class="header-actions">
        <input
          v-model="store.catalogKeyword"
          type="text"
          placeholder="搜索表名..."
          class="search-input"
          @input="filterTables"
        />
      </div>
    </div>

    <div class="catalog-layout">
      <!-- 左栏：表列表 -->
      <div class="table-list-section">
        <div class="section-header">
          <h3>数据资产目录（按层级）</h3>
          <span class="table-count">{{ filteredTables.length }} 张表</span>
        </div>
        <div class="table-list">
          <div
            v-for="table in filteredTables"
            :key="table.tableName"
            class="table-item"
            :class="{ active: store.selectedTable?.tableName === table.tableName }"
            @click="selectTable(table)"
          >
            <div class="table-icon">{{ table.schema?.toUpperCase() }}</div>
            <div class="table-info">
              <div class="table-name">{{ table.schema }}.{{ table.tableName }}</div>
              <div class="table-meta">
                {{ formatNumber(table.rowCount) }} 行 · {{ table.fieldCount }} 字段
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：表详情 -->
      <div class="table-detail-section">
        <div v-if="store.selectedTable" class="detail-content">
          <div class="detail-header">
            <h3>{{ store.selectedTable.schema }}.{{ store.selectedTable.tableName }}</h3>
            <button type="button" class="btn btn-query" @click="openQueryModal">查询数据</button>
          </div>

          <div class="fields-section">
            <h4>字段信息</h4>
            <div class="fields-table">
              <table>
                <thead>
                  <tr>
                    <th>字段名</th>
                    <th>类型</th>
                    <th>可空</th>
                    <th>备注</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="field in fields" :key="field.name">
                    <td class="field-name">{{ field.name }}</td>
                    <td class="field-type">{{ field.type }}</td>
                    <td class="field-nullable">
                      <span :class="field.nullable ? 'nullable-yes' : 'nullable-no'">
                        {{ field.nullable ? '是' : '否' }}
                      </span>
                    </td>
                    <td class="field-desc">{{ field.description || '-' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="preview-section">
            <h4>数据预览（前 5 行）</h4>
            <div class="preview-table">
              <table>
                <thead>
                  <tr>
                    <th v-for="col in previewColumns" :key="col">{{ col }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, idx) in previewRows" :key="idx">
                    <td v-for="(val, colIdx) in row" :key="colIdx">{{ formatValue(val) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <div class="empty-icon">目</div>
          <p>请从左侧选择一张数据表</p>
        </div>
      </div>
    </div>

    <div v-if="showQueryModal" class="modal-overlay" @click="closeQueryModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>数据查询</h3>
          <button type="button" class="modal-close" @click="closeQueryModal">关闭</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>选择字段</label>
            <div class="field-checkboxes">
              <label v-for="field in fields" :key="field.name" class="checkbox-item">
                <input v-model="selectedFields" type="checkbox" :value="field.name" />
                {{ field.name }}
              </label>
            </div>
          </div>
          <div class="form-group">
            <label>限制行数</label>
            <input v-model.number="queryLimit" type="number" min="1" max="1000" class="form-input" />
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-cancel" @click="closeQueryModal">取消</button>
          <button type="button" class="btn btn-primary" @click="executeQuery" :disabled="querying">
            {{ querying ? '查询中...' : '执行查询' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showResultModal" class="modal-overlay" @click="closeResultModal">
      <div class="modal-content large" @click.stop>
        <div class="modal-header">
          <h3>查询结果（共 {{ queryResult?.totalRows || 0 }} 行）</h3>
          <button type="button" class="modal-close" @click="closeResultModal">关闭</button>
        </div>
        <div class="modal-body">
          <div class="result-table">
            <table>
              <thead>
                <tr>
                  <th v-for="col in queryResult?.columns" :key="col">{{ col }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, idx) in queryResult?.rows" :key="idx">
                  <td v-for="(val, colIdx) in row" :key="colIdx">{{ formatValue(val) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-primary" @click="closeResultModal">关闭</button>
        </div>
      </div>
    </div>

    <div class="hot-fields-section">
      <h3>字段热度排行</h3>
      <div class="hot-fields-list">
        <div v-for="field in hotFields" :key="field.fieldName" class="hot-field-item">
          <span class="hot-field-rank">{{ field.rank }}</span>
          <span class="hot-field-name">{{ field.fieldName }}</span>
          <span class="hot-field-count">{{ field.queryCount }} 次查询</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useDashboardStore } from '@/stores/dashboardStore'
import { dashboardApi } from '@/services/dashboardApi'

const store = useDashboardStore()

const tables = ref([])
const filteredTables = ref([])
const fields = ref([])
const previewRows = ref([])
const previewColumns = ref([])

const showQueryModal = ref(false)
const showResultModal = ref(false)
const selectedFields = ref([])
const queryLimit = ref(20)
const querying = ref(false)
const queryResult = ref(null)

const hotFields = ref([])

onMounted(async () => {
  await loadTables()
  await loadHotFields()
})

async function loadTables() {
  try {
    tables.value = await dashboardApi.getCatalogTables()
    filterTables()
  } catch (error) {
    console.error('Failed to load tables:', error)
  }
}

function filterTables() {
  const keyword = store.catalogKeyword.toLowerCase()
  filteredTables.value = tables.value.filter((t) => t.tableName.toLowerCase().includes(keyword))
}

async function selectTable(table) {
  store.setSelectedTable(table)
  await loadTableFields(table.schema, table.tableName)
}

async function loadTableFields(schema, tableName) {
  try {
    const data = await dashboardApi.getCatalogFields(schema, tableName)
    fields.value = data.fields
    if (data.preview && data.preview.length > 0) {
      previewColumns.value = Object.keys(data.preview[0])
      previewRows.value = data.preview
    }
  } catch (error) {
    console.error('Failed to load fields:', error)
  }
}

async function loadHotFields() {
  try {
    hotFields.value = await dashboardApi.getHotFields()
  } catch (error) {
    console.error('Failed to load hot fields:', error)
  }
}

function openQueryModal() {
  selectedFields.value = fields.value.slice(0, 5).map((f) => f.name)
  queryLimit.value = 20
  showQueryModal.value = true
}

function closeQueryModal() {
  showQueryModal.value = false
}

async function executeQuery() {
  querying.value = true
  try {
    queryResult.value = await dashboardApi.queryCatalog({
      schema: store.selectedTable.schema,
      tableName: store.selectedTable.tableName,
      fields: selectedFields.value,
      limit: queryLimit.value,
    })
    showQueryModal.value = false
    showResultModal.value = true
  } catch (error) {
    console.error('Query failed:', error)
  } finally {
    querying.value = false
  }
}

function closeResultModal() {
  showResultModal.value = false
  queryResult.value = null
}

function formatNumber(num) {
  if (!num) return '0'
  return num.toLocaleString()
}

function formatValue(val) {
  if (val === null || val === undefined) return '-'
  if (typeof val === 'boolean') return val ? '是' : '否'
  return String(val)
}
</script>

<style scoped>
.catalog-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px;
  background: #0a0e1a;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 16px;
  border-bottom: 1px solid rgba(0, 204, 255, 0.1);
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #0cf;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.search-input {
  padding: 8px 16px;
  background: rgba(0, 204, 255, 0.05);
  border: 1px solid rgba(0, 204, 255, 0.2);
  border-radius: 4px;
  color: #fff;
  font-size: 14px;
  width: 200px;
}

.search-input:focus {
  outline: none;
  border-color: #0cf;
}

.catalog-layout {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
  padding-top: 16px;
}

.table-list-section {
  width: 320px;
  background: rgba(0, 204, 255, 0.03);
  border-radius: 8px;
  border: 1px solid rgba(0, 204, 255, 0.1);
  display: flex;
  flex-direction: column;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 204, 255, 0.1);
}

.section-header h3 {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  margin: 0;
}

.table-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.table-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.table-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.table-item:hover {
  background: rgba(0, 204, 255, 0.1);
}

.table-item.active {
  background: rgba(0, 204, 255, 0.2);
  border: 1px solid rgba(0, 204, 255, 0.3);
}

.table-icon {
  font-size: 14px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  background: rgba(0, 204, 255, 0.15);
  color: #0cf;
  font-weight: 600;
}

.table-info {
  flex: 1;
}

.table-name {
  font-size: 13px;
  color: #0cf;
  font-weight: 500;
  margin-bottom: 2px;
}

.table-meta {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.table-detail-section {
  flex: 1;
  background: rgba(0, 204, 255, 0.03);
  border-radius: 8px;
  border: 1px solid rgba(0, 204, 255, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.detail-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 204, 255, 0.1);
}

.detail-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin: 0;
}

.btn {
  padding: 6px 16px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.btn-query {
  background: rgba(0, 204, 255, 0.2);
  border: 1px solid rgba(0, 204, 255, 0.4);
  color: #0cf;
}

.btn-query:hover {
  background: rgba(0, 204, 255, 0.3);
}

.fields-section,
.preview-section {
  padding: 16px;
  border-bottom: 1px solid rgba(0, 204, 255, 0.1);
}

.fields-section h4,
.preview-section h4 {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0 0 12px;
}

.fields-table,
.preview-table {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

th {
  text-align: left;
  padding: 8px 12px;
  color: rgba(255, 255, 255, 0.5);
  font-weight: 500;
  border-bottom: 1px solid rgba(0, 204, 255, 0.1);
}

td {
  padding: 8px 12px;
  border-bottom: 1px solid rgba(0, 204, 255, 0.05);
  color: rgba(255, 255, 255, 0.8);
}

.field-name {
  color: #0cf;
  font-weight: 500;
}

.field-type {
  color: rgba(255, 255, 255, 0.6);
}

.nullable-yes {
  color: #fa0;
}

.nullable-no {
  color: #0f0;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.3);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: #0f1520;
  border: 1px solid rgba(0, 204, 255, 0.2);
  border-radius: 8px;
  width: 500px;
  max-width: 90vw;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.modal-content.large {
  width: 800px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(0, 204, 255, 0.1);
}

.modal-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
  cursor: pointer;
}

.modal-close:hover {
  color: #fff;
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 8px;
}

.field-checkboxes {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.checkbox-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
}

.form-input {
  width: 100%;
  padding: 8px 12px;
  background: rgba(0, 204, 255, 0.05);
  border: 1px solid rgba(0, 204, 255, 0.2);
  border-radius: 4px;
  color: #fff;
  font-size: 13px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid rgba(0, 204, 255, 0.1);
}

.btn-cancel {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.7);
}

.btn-primary {
  background: #0cf;
  border: none;
  color: #000;
  font-weight: 600;
}

.btn-primary:hover:not(:disabled) {
  background: #0db;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.result-table {
  max-height: 400px;
  overflow: auto;
}

.hot-fields-section {
  padding-top: 16px;
  border-top: 1px solid rgba(0, 204, 255, 0.1);
}

.hot-fields-section h3 {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 12px;
}

.hot-fields-list {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.hot-field-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(0, 204, 255, 0.05);
  border: 1px solid rgba(0, 204, 255, 0.15);
  border-radius: 4px;
  white-space: nowrap;
}

.hot-field-rank {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  background: #0cf;
  color: #000;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 600;
}

.hot-field-name {
  color: #0cf;
  font-size: 12px;
}

.hot-field-count {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}
</style>
