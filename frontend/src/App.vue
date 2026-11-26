<script setup>
import { ref, onMounted } from 'vue'

const projectId = 1
const applicantId = 1

const list = ref([])
const summary = ref(null)
const loading = ref(true)
const error = ref('')
const message = ref('')
const msgError = ref('') 

const form = ref({ purpose: '', amount: '' })
const submitting = ref(false)

const stateText = {
  DRAFT: '草稿',
  PENDING_L1: '待审批',
  APPROVED: '已批准',
  SETTLED: '已结算',
  REJECTED: '已拒绝',
  CANCELED: '已取消',
}

async function api(url, options) {
  const res = await fetch(url, options)
  const text = await res.text()
  const data = text ? JSON.parse(text) : null
  if (!res.ok) throw new Error(data?.error || `请求失败：${res.status}`)
  return data
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    list.value = await api(`/api/applications?projectId=${projectId}`)
        summary.value = await api(`/api/projects/${projectId}/summary`)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function submit() {
  message.value = ''
  if (!form.value.purpose.trim()) {
    message.value = '请填写用途'
    return
  }
  submitting.value = true
  msgError.value = ''
  try {
    await api('/api/applications', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        projectId,
        applicantId,
        purpose: form.value.purpose,
        amount: form.value.amount,
      }),
    })
    message.value = '提交成功'
    form.value = { purpose: '', amount: '' }
    await load()
  } catch (e) {
    msgError.value = e.message
  } finally {
    submitting.value = false
  }
}

async function run(id, action, body, okText) {
  message.value = ''
  msgError.value = ''
  try {
    await api(`/api/applications/${id}/${action}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
    message.value = okText
    await load()
  } catch (e) {
    message.value = e.message
  }
}

function approve(a) {
  const v = prompt('批准金额（不超过 ' + a.applyAmount + '）', a.applyAmount)
  if (v === null) return
  run(a.id, 'approve', { amount: v }, '已批准')
}

function settle(a) {
  const v = prompt('实际花费（不超过 ' + a.approvedAmount + '）', a.approvedAmount)
  if (v === null) return
  run(a.id, 'settle', { amount: v }, '已结算')
}

function reject(a) {
  const v = prompt('驳回原因')
  if (v === null) return
  run(a.id, 'reject', { reason: v }, '已驳回')
}

function cancel(a) {
  const v = prompt('取消原因')
  if (v === null) return
  run(a.id, 'cancel', { reason: v }, '已取消')
}

onMounted(load)
</script>

<template>
  <div class="page">
    <h1>经费申请 · 项目 {{ projectId }}</h1>
        <div v-if="summary" class="summary">
      <span>总额 <b>{{ summary.total }}</b></span>
      <span>可用 <b>{{ summary.available }}</b></span>
      <span>冻结 <b>{{ summary.frozen }}</b></span>
      <span>已用 <b>{{ summary.used }}</b></span>
    </div>

    <section class="form">
      <input v-model="form.purpose" placeholder="用途" />
      <input v-model="form.amount" placeholder="金额" type="number" step="0.01" />
      <button :disabled="submitting" @click="submit">
        {{ submitting ? '提交中…' : '提交申请' }}
      </button>
    </section>

    <p v-if="message" class="msg">{{ message }}</p>
    <p v-if="loading" class="tip">加载中…</p>
    <p v-else-if="error" class="msg error">{{ error }}</p>
    <p v-if="msgError" class="msg error">{{ msgError }}</p>

    <table v-else>
      <thead>
        <tr>
          <th>编号</th><th>申请人</th><th>用途</th>
          <th>申请</th><th>批准</th><th>实际</th>
          <th>状态</th><th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="a in list" :key="a.id">
          <td>{{ a.id }}</td>
          <td>{{ a.applicant?.name }}</td>
          <td>{{ a.purpose }}</td>
          <td>{{ a.applyAmount ?? '—' }}</td>
          <td>{{ a.approvedAmount ?? '—' }}</td>
          <td>{{ a.actualAmount ?? '—' }}</td>
          <td>{{ stateText[a.state] ?? a.state }}</td>
          <td class="ops">
            <template v-if="a.state === 'PENDING_L1'">
              <button @click="approve(a)">批准</button>
              <button @click="reject(a)">驳回</button>
              <button @click="cancel(a)">取消</button>
            </template>
            <template v-else-if="a.state === 'APPROVED'">
              <button @click="settle(a)">结算</button>
              <button @click="cancel(a)">取消</button>
            </template>
            <span v-else class="tip">—</span>
          </td>
        </tr>
        <tr v-if="list.length === 0">
          <td colspan="8" class="tip">还没有申请</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.page { max-width: 1100px; margin: 40px auto; font-family: system-ui, sans-serif; }
h1 { font-size: 20px; margin-bottom: 16px; }
.form { display: flex; gap: 8px; margin-bottom: 12px; }
.form input { padding: 6px 10px; border: 1px solid #ccc; border-radius: 4px; }
.form input:first-child { flex: 1; }
.form input:nth-child(2) { width: 120px; }
button { padding: 6px 12px; border: 1px solid #ccc; background: #fff; border-radius: 4px; cursor: pointer; }
button:hover { background: #f0f0f0; }
button:disabled { opacity: .5; cursor: default; }
table { width: 100%; border-collapse: collapse; font-size: 14px; }
th, td { border: 1px solid #ddd; padding: 8px 10px; text-align: left; }
th { background: #f5f5f5; font-weight: 600; }
tbody tr:nth-child(even) { background: #fafafa; }
.ops { display: flex; gap: 6px; }
.ops button { padding: 3px 8px; font-size: 13px; }
.msg { color: #060; margin: 8px 0; }
.msg.error { color: #c00; }
.tip { color: #666; }
.summary {
  display: flex;
  gap: 28px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #f8f8f8;
  border: 1px solid #eee;
  border-radius: 6px;
  font-size: 14px;
}
.summary b { font-size: 16px; color: #333; }
</style>