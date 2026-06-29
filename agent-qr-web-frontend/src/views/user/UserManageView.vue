<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import type { UserInfo } from '@/types'
import { DEPARTMENTS } from '@/types'
import { userApi } from '@/api/user'
import UserTable from '@/components/user/UserTable.vue'
import UserFormDialog from '@/components/user/UserFormDialog.vue'
import Pagination from '@/components/common/Pagination.vue'

const { t } = useI18n()

// --- 列表数据 ---
const users = ref<UserInfo[]>([])
const total = ref(0)
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')

// --- 筛选 ---
const filterDepartment = ref('')
const filterTitle = ref('')

// --- 弹窗控制 ---
const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingUser = ref<UserInfo | undefined>(undefined)

async function fetchUsers() {
  loading.value = true
  try {
    const res = await userApi.listUsers({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      department: filterDepartment.value || undefined,
      title: filterTitle.value || undefined,
    })
    users.value = res.data.records
    total.value = res.data.total
  } catch {
    // 错误已在拦截器中统一处理
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchUsers()
}

function handlePageChange() {
  fetchUsers()
}

function handleCreate() {
  formMode.value = 'create'
  editingUser.value = undefined
  formVisible.value = true
}

function handleEdit(user: UserInfo) {
  formMode.value = 'edit'
  editingUser.value = user
  formVisible.value = true
}

async function handleToggleStatus(user: UserInfo) {
  const newStatus = user.status === 1 ? 0 : 1
  const actionText = newStatus === 0 ? t('common.disable') : t('common.enable')
  try {
    await ElMessageBox.confirm(
      t('user.confirmToggleStatus', { action: actionText, username: user.username }),
      t('common.confirmAction'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning',
      },
    )
    await userApi.toggleStatus(user.id, newStatus)
    ElMessage.success(t('user.toggleStatusSuccess', { action: actionText }))
    fetchUsers()
  } catch {
    // 取消操作或接口失败
  }
}

function handleFormSuccess() {
  fetchUsers()
}

onMounted(() => {
  fetchUsers()
})
</script>

<template>
  <div class="user-manage">
    <h2 class="user-manage__title">{{ $t('user.title') }}</h2>

    <!-- 操作栏 -->
    <div class="user-manage__toolbar">
      <el-button type="primary" v-permission="'canManageUsers'" @click="handleCreate">+ {{ $t('user.createUser') }}</el-button>
      <div class="user-manage__filters">
        <el-select
          v-model="filterDepartment"
          :placeholder="$t('user.filterDepartment')"
          clearable
          style="width: 140px"
          @change="handleSearch"
        >
          <el-option :label="$t('common.all')" value="" />
          <el-option
            v-for="item in DEPARTMENTS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select
          v-model="filterTitle"
          :placeholder="$t('user.filterTitle')"
          clearable
          style="width: 120px"
          @change="handleSearch"
        >
          <el-option :label="$t('common.all')" value="" />
          <el-option :label="$t('user.titleEmployee')" value="employee" />
          <el-option :label="$t('user.titleManager')" value="manager" />
          <el-option :label="$t('user.titleDirector')" value="director" />
        </el-select>
        <el-input
          v-model="searchKeyword"
          :placeholder="$t('user.searchPlaceholder')"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #suffix>
            <el-icon
              class="search-icon"
              style="cursor: pointer"
              @click="handleSearch"
            >
              <svg viewBox="0 0 1024 1024" width="1em" height="1em">
                <path
                  fill="currentColor"
                  d="M795.904 750.72l124.992 124.928a32 32 0 0 1-45.248 45.248L750.656 795.904a416 416 0 1 1 45.248-45.248zM448 768a320 320 0 1 0 0-640 320 320 0 0 0 0 640z"
                />
              </svg>
            </el-icon>
          </template>
        </el-input>
      </div>
    </div>

    <!-- 表格 -->
    <UserTable
      :users="users"
      :loading="loading"
      @edit="handleEdit"
      @toggle-status="handleToggleStatus"
    />

    <!-- 分页 -->
    <Pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      @change="handlePageChange"
    />

    <!-- 弹窗 -->
    <UserFormDialog
      v-model:visible="formVisible"
      :mode="formMode"
      :user-data="editingUser"
      @success="handleFormSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
.user-manage {
  &__title {
    font-size: var(--font-size-xl);
    font-weight: var(--font-weight-semibold);
    color: $text-primary;
    margin-bottom: var(--space-5);
  }

  &__toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--space-4);
  }

  &__filters {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }
}

.search-icon {
  color: #999;
  &:hover {
    color: var(--el-color-primary);
  }
}
</style>
