<template>
  <div class="manage-page">
    <div class="page-head">
      <h2 class="page-title">分类管理</h2>
      <el-button type="primary" @click="openDialog()">+ 新增分类</el-button>
    </div>

    <el-table :data="categories" stripe>
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="图标" width="90">
        <template #default="{ row }"><span style="font-size: 22px">{{ row.icon }}</span></template>
      </el-table-column>
      <el-table-column label="分类名称" prop="name" />
      <el-table-column label="排序" prop="sort" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
          <el-button text type="danger" size="small" @click="removeCategory(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑分类' : '新增分类'" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" placeholder="emoji 或图片URL" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminCategories, saveCategory, updateCategory, deleteCategory } from '../../api'

const categories = ref([])
const dialog = ref(false)
const form = reactive({ id: null, name: '', icon: '', sort: 0, status: 1 })

const load = async () => {
  const { data } = await adminCategories()
  categories.value = data
}

const openDialog = (row) => {
  Object.assign(form, row || { id: null, name: '', icon: '', sort: 0, status: 1 })
  dialog.value = true
}

const save = async () => {
  if (!form.name) {
    ElMessage.warning('请填写分类名称')
    return
  }
  if (form.id) {
    await updateCategory(form.id, { ...form })
  } else {
    await saveCategory({ ...form })
  }
  dialog.value = false
  ElMessage.success('保存成功')
  load()
}

const removeCategory = async (row) => {
  await ElMessageBox.confirm(`确定删除「${row.name}」分类吗？`, '提示', { type: 'warning' })
  await deleteCategory(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { font-size: 22px; }
</style>
