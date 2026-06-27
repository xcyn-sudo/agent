<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { knowledgeApi } from '@/api/knowledge'
import { SENSITIVITY_LEVELS } from '@/types'
import { formatDomain } from '@/utils/format'

const props = defineProps<{
  visible: boolean
  allowedDomains: string[]
  maxClearanceLevel: number
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const dialogVisible = ref(props.visible)
watch(() => props.visible, (val) => {
  dialogVisible.value = val
})
watch(dialogVisible, (val) => {
  emit('update:visible', val)
})

const fileList = ref<any[]>([])
const title = ref('')
const domain = ref('')
const sensitivityLevel = ref<number>(0)
const uploading = ref(false)

const allowedTypes = ['.pdf', '.docx', '.txt', '.md']
const maxSize = 50 * 1024 * 1024

const availableSensitivityLevels = computed(() =>
  SENSITIVITY_LEVELS.filter(s => s.value <= props.maxClearanceLevel)
)

function beforeUpload(file: File) {
  const ext = '.' + file.name.split('.').pop()?.toLowerCase()
  if (!allowedTypes.includes(ext)) {
    ElMessage.warning(`仅支持 ${allowedTypes.join('/')} 格式文件`)
    return false
  }
  if (file.size > maxSize) {
    ElMessage.warning('文件大小不能超过 50MB')
    return false
  }
  return true
}

function handleRemove() {
  fileList.value = []
}

function handleClose() {
  dialogVisible.value = false
  fileList.value = []
  title.value = ''
  domain.value = ''
  sensitivityLevel.value = 0
}

async function handleUpload() {
  if (fileList.value.length === 0) {
    ElMessage.warning('请先选择文件')
    return
  }
  const file = fileList.value[0].raw
  if (!file) {
    ElMessage.warning('文件无效')
    return
  }
  if (!domain.value) {
    ElMessage.warning('请选择业务域')
    return
  }
  uploading.value = true
  try {
    await knowledgeApi.upload(file, title.value || undefined, domain.value, sensitivityLevel.value)
    ElMessage.success('上传成功')
    emit('success')
    handleClose()
  } catch {
    // 错误已在拦截器中统一处理
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="上传文档"
    width="520px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-upload
      v-model:file-list="fileList"
      class="upload-area"
      drag
      :auto-upload="false"
      :before-upload="beforeUpload"
      :limit="1"
      :on-remove="handleRemove"
    >
      <el-icon class="el-icon--upload">
        <UploadFilled />
      </el-icon>
      <div class="el-upload__text">
        将文件拖到此处，或<em>点击上传</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          支持 PDF、DOCX、TXT、MD 格式，大小不超过 50MB
        </div>
      </template>
    </el-upload>

    <el-input
      v-model="title"
      placeholder="默认使用文件名"
      clearable
      class="form-item"
    />

    <el-select
      v-model="domain"
      placeholder="请选择业务域"
      class="form-item"
    >
      <el-option
        v-for="d in props.allowedDomains"
        :key="d"
        :label="formatDomain(d)"
        :value="d"
      />
    </el-select>

    <el-select
      v-model="sensitivityLevel"
      placeholder="请选择密级"
      class="form-item"
    >
      <el-option
        v-for="s in availableSensitivityLevels"
        :key="s.value"
        :label="s.label"
        :value="s.value"
      />
    </el-select>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose" :disabled="uploading">取消</el-button>
        <el-button type="primary" @click="handleUpload" :loading="uploading">
          确认上传
        </el-button>
      </span>
    </template>

    <div v-if="uploading" class="upload-overlay">
      <el-icon class="is-loading" :size="32">
        <Loading />
      </el-icon>
      <span>正在上传...</span>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.upload-area {
  width: 100%;
}

.form-item {
  margin-top: 16px;
  width: 100%;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  border-radius: 8px;
  z-index: 10;
  color: var(--el-color-primary);
}
</style>
