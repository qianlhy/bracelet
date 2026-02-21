<template>
  <view class="page">
    <!-- 顶部状态栏 -->
    <view class="header">
      <view class="header-left">
        <view class="stat-box">
          <text class="stat-text">{{ beads.length }}颗 {{ weightG }}g</text>
        </view>
      </view>
      
      <view class="header-right">
        <view class="price-box">
          <text class="price-label">合计</text>
          <text class="price-num">¥{{ totalPrice }}</text>
        </view>
        <view class="action-btns">

          <button 
            class="checkout-btn" 
            :disabled="!beads.length" 
            @click="showPreview"
          >
            结算
          </button>
        </view>
      </view>
    </view>

    <!-- 主画布区域 -->
    <view class="main-area">
      <!-- 提示图标 -->
      <view class="tip-icon" @click="showTips">!</view>
      
      <!-- 中心画布 -->
      <view class="canvas-container">
        <view 
          class="canvas"
          @touchstart="onTouchStart"
          @touchmove.stop.prevent="onTouchMove"
          @touchend="onTouchEnd"
        >
          <view :style="{transform: `scale(${canvasScale})`, transformOrigin: 'center center', width: '100%', height: '100%'}">
          <!-- 绳子 -->
          <view class="rope-circle" :style="{width: layoutRadius * 2 + 'rpx', height: layoutRadius * 2 + 'rpx', zIndex: 5}"></view>
          
          <!-- 中心Logo -->
          <view class="center-logo" :style="{zIndex: 2}">
            <image class="logo-img" :src="logoPath" mode="widthFix" />
            <text class="logo-name">念初手作</text>
          </view>
          
          <!-- 珠子 -->
          <view 
            v-for="(b, i) in beads" 
            :key="b._id" 
            class="bead"
            :class="{
              'is-new': b.isNew,
              'is-mirrored': b.mirrored,
              'is-pendant': isPendant(b),
              'is-dragging': draggingIndex === i,
              'is-drag-target': isDragging && dragTargetIndex === i,
              'is-swap-source': swapMode && firstSwapIndex === i,
              'is-swap-target': swapMode && firstSwapIndex !== i
            }"
            :style="getBeadStyle(b, i)"
            :data-index="i"
            @click="onBeadTap(i)"
            @longpress="startDrag(i)"
          >
             <image v-if="b.imageUrl && !b.loadFailed" :src="b.imageUrl" mode="aspectFit" class="bead-img" @error="onBeadImageError(b, i)"></image>
             <view v-else class="bead-placeholder" :style="{background: b.color || '#f0f0f0'}"></view>
             <view v-if="swapMode && firstSwapIndex === i" class="swap-indicator">1</view>
          </view>
          </view>
        </view>
      </view>
      
      <!-- 飞行动画 -->
      <view 
        v-if="flyingBead" 
        class="flying-bead" 
        :style="flyingBeadStyle"
      >
        <image 
          v-if="flyingBead.imageUrl" 
          :src="flyingBead.imageUrl" 
          mode="aspectFit" 
          style="width:100%;height:100%;border-radius:50%;"
        ></image>
      </view>

      <!-- 底部工具栏 -->
      <view class="toolbar-container" :class="{ collapsed: isToolbarCollapsed }" @click="onToolbarContainerClick">
        <view class="tool-bar">
          <view class="tool-item" :class="{ active: showSizeSelector }" @click="showSizeSelector = true">
            <text class="tool-icon-img">📏</text>
            <text class="tool-text">{{ selectedSize }}cm</text>
          </view>
          <view class="tool-item" :class="{ active: isAutoArranged }" @click="toggleAutoArrange">
            <text class="tool-icon-img">⊞</text>
            <text class="tool-text">均匀</text>
          </view>
          <view class="tool-item" @click="mirrorAllBeads">
            <text class="tool-icon-img">⇄</text>
            <text class="tool-text">镜像</text>
          </view>
          <view class="tool-item" @click="undoLastBead">
            <text class="tool-icon-img">↩</text>
            <text class="tool-text">撤销</text>
          </view>
          <view class="tool-item" @click="confirmClear">
            <text class="tool-icon-img">🗑</text>
            <text class="tool-text">清空</text>
          </view>
        </view>
        <view class="tool-collapse-hit" @click.stop="toggleToolbar">
          <view class="tool-collapse-btn">
            <text class="collapse-icon">{{ isToolbarCollapsed ? '›' : '‹' }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部商品选择区 -->
    <view class="goods-section">
<!-- 暂时屏蔽以排查 wx://not-found 错误 -->
<!-- <canvas type="2d" id="exportCanvas" class="export-canvas" style="width: 560rpx; height: 560rpx; position: fixed; left: -9999px;"></canvas> -->      <view class="section-header">
        <scroll-view class="category-tabs" scroll-x :show-scrollbar="false">
          <view
            class="cat-tab"
            :class="{active: activeCategory === cat.keyCode}"
            v-for="cat in displayCategories"
            :key="cat.keyCode"
            @click="switchCategory(cat.keyCode)"
          >
            <text>{{ cat.name }}</text>
          </view>
        </scroll-view>
        <!-- 搜索框 -->
        <view class="search-box">
          <view class="search-input-wrapper">
            <text class="search-icon">🔍</text>
            <input
              class="search-input"
              type="text"
              v-model="searchKeyword"
              placeholder="搜索珠子/配饰/吊坠"
              @input="onSearchInput"
              @focus="onSearchFocus"
              @blur="onSearchBlur"
            />
            <text v-if="searchKeyword" class="search-clear" @click="clearSearch">✕</text>
          </view>
          <!-- 搜索结果下拉框 -->
          <view v-if="showSearchResults && searchResults.length > 0" class="search-dropdown">
            <scroll-view class="search-dropdown-scroll" scroll-y :show-scrollbar="false">
              <view
                v-for="item in searchResults"
                :key="item.id"
                class="search-result-item"
                @click="selectSearchResult(item)"
              >
                <image v-if="item.imageUrl" class="search-result-img" :src="item.imageUrl" mode="aspectFit" />
                <view v-else class="search-result-color" :style="{background: item.color || '#e8e8e8'}"></view>
                <view class="search-result-info">
                  <text class="search-result-name">{{ item.title }}</text>
                  <text class="search-result-category">{{ item.categoryName }}</text>
                </view>
                <text class="search-result-price">¥{{ item.price }}</text>
              </view>
            </scroll-view>
          </view>
          <!-- 无搜索结果提示 -->
          <view v-else-if="showSearchResults && searchKeyword && !searchResults.length" class="search-dropdown">
            <view class="search-empty">未找到相关商品</view>
          </view>
        </view>
      </view>
      
      <view class="section-body">
        <!-- 侧边栏筛选 -->
        <scroll-view class="color-sidebar" scroll-y :show-scrollbar="false">
          <!-- 子分类/色系 (统一展示) -->
          <view class="sidebar-group">
            <view
              class="color-tag"
              :class="{active: !activeColor}"
              @click="switchColor('')"
            >
              全部
            </view>
            <view
              class="color-tag"
              :class="{active: activeColor === getSubCategoryKey(sub)}"
              v-for="sub in currentSubCategories"
              :key="getSubCategoryKey(sub)"
              @click="switchColor(getSubCategoryKey(sub))"
            >
              {{ sub.name }}
            </view>
          </view>
        </scroll-view>
        
        <!-- 商品列表 -->
        <scroll-view class="product-area" scroll-y :show-scrollbar="false" :scroll-top="productScrollTop" @scrolltolower="onProductScrollToLower">
          <view v-if="loading && page === 1" class="state-box">
            <view class="loading-dot"></view>
            <text>加载中</text>
          </view>
          <view v-else-if="!goods.length" class="state-box">
            <text>暂无商品</text>
          </view>
          <view v-else class="product-list">
            <view 
              class="product-card" 
              :class="{active: clickedId === g.id, soldout: g.stock === 0}"
              v-for="g in goods" 
              :key="g.id" 
              @click="addBead(g, $event)"
            >
              <view class="p-visual">
                <view class="p-bead" :style="{background: g.color || 'transparent'}">
                <image 
                  v-if="g.imageUrl" 
                  :src="g.imageUrl" 
                  mode="aspectFit" 
                  class="p-img"
                  :class="{loaded: g.loaded}"
                  lazy-load
                  @load="g.loaded = true"
                />
              </view>
                <view class="p-stock">
                  库存{{ g.stock }}
                </view>
              </view>
              <text class="p-name">{{ g.title }}</text>
              <text class="p-price">¥{{ g.price }}</text>
              <text class="p-size" v-if="g.size">{{ g.size }}mm</text>
            </view>
          </view>
          <!-- <view v-if="loading && page > 1" class="loading-more">
            <text>加载更多...</text>
          </view> -->
        </scroll-view>
      </view>
    </view>

    <!-- 尺寸选择弹窗 -->
    <view v-if="showSizeSelector" class="mask" @click="showSizeSelector = false">
      <view class="popup" @click.stop>
        <view class="popup-header">
          <text class="popup-title">选择手围</text>
          <text class="popup-close" @click="showSizeSelector = false">✕</text>
        </view>
        <view class="popup-body">
          <!-- 戴法选择 -->
          <view class="wear-style-section">
            <text class="section-label">选择戴法</text>
            <view class="wear-style-options">
              <view 
                class="wear-style-opt" 
                :class="{selected: wearStyle === 'single'}" 
                @click="selectWearStyle('single')"
              >
                <view class="style-radio">
                  <view v-if="wearStyle === 'single'" class="style-radio-inner"></view>
                </view>
                <view class="style-info">
                  <text class="style-name">单圈</text>
                  <text class="style-desc">适合日常佩戴</text>
                </view>
              </view>
              <view 
                class="wear-style-opt" 
                :class="{selected: wearStyle === 'double'}" 
                @click="selectWearStyle('double')"
              >
                <view class="style-radio">
                  <view v-if="wearStyle === 'double'" class="style-radio-inner"></view>
                </view>
                <view class="style-info">
                  <text class="style-name">双圈</text>
                  <text class="style-desc">适合宽松手围</text>
                </view>
              </view>
            </view>
          </view>
          <!-- 尺寸选择 -->
          <view class="size-section">
            <text class="section-label">手围尺寸</text>
            <view class="size-options">
              <view 
                class="size-opt" 
                :class="{selected: selectedSize === s}" 
                v-for="s in currentSizeOptions" 
                :key="s" 
                @click="selectSize(s)"
              >
                <text class="opt-num">{{ s }}</text>
                <text class="opt-unit">cm</text>
              </view>
            </view>
          </view>
        </view>
        <button class="popup-btn" @click="confirmSize">确定</button>
      </view>
    </view>

    <!-- 订单预览弹窗 -->
    <view v-if="showOrderPreview" class="mask" @click="showOrderPreview = false">
      <view class="popup preview-popup" @click.stop>
        <view class="popup-header">
          <text class="popup-title">确认订单</text>
          <text class="popup-close" @click="showOrderPreview = false">✕</text>
        </view>
        <view class="preview-body">
          <view class="preview-meta">
            <text>手围 {{ selectedSize }}cm</text>
            <text>{{ beads.length }}颗珠子</text>
            <text>约{{ weightG }}g</text>
          </view>
          <view class="preview-items">
            <view class="pv-item" v-for="(item, i) in orderItems" :key="i">
              <text class="pv-name">{{ item.title }}</text>
              <text class="pv-qty">×{{ item.quantity }}</text>
              <text class="pv-price">¥{{ (item.price * item.quantity).toFixed(2) }}</text>
            </view>
          </view>
          <view class="preview-total">
            <text>合计</text>
            <text class="pv-total-num">¥{{ totalPrice }}</text>
          </view>
        </view>
        <view class="preview-actions">
          <button class="act-cancel" @click="showOrderPreview = false">继续编辑</button>
          <button class="act-submit" @click="submitOrder">提交订单</button>
        </view>
      </view>
    </view>

    <!-- 首次进入引导 -->
    <view v-if="showInitGuide" class="mask">
      <view class="popup guide-popup" @click.stop>
        <view class="guide-icon">✨</view>
        <text class="guide-title">欢迎来到DIY设计台</text>
        <text class="guide-desc">请先选择您的手围尺寸</text>
        <view class="size-options compact">
          <view 
            class="size-opt" 
            :class="{selected: selectedSize === s}" 
            v-for="s in sizeOptions" 
            :key="s" 
            @click="selectSize(s)"
          >
            <text class="opt-num">{{ s }}</text>
            <text class="opt-unit">cm</text>
          </view>
        </view>
        <button class="popup-btn" @click="startDesign">开始设计</button>
      </view>
    </view>
    
    <!-- 操作指南弹窗 -->
    <view v-if="showHelpPopup" class="mask" @click="closeHelpPopup">
      <view class="popup help-popup" @click.stop>
        <view class="popup-header">
          <view class="header-deco">✨</view>
          <text class="popup-title">操作指南</text>
          <text class="popup-close" @click="closeHelpPopup">✕</text>
        </view>
        <view class="help-grid">
          <view class="help-item">
            <view class="help-icon-box">
              <view class="demo-bead"></view>
              <view class="demo-hand">👆</view>
            </view>
            <text class="help-text">点击下方珠子添加</text>
          </view>
          <view class="help-item">
            <view class="help-icon-box">
              <view class="demo-circle"></view>
              <view class="demo-drag-hand">👆</view>
            </view>
            <text class="help-text">长按拖动调整位置</text>
          </view>
          <view class="help-item">
            <view class="help-icon-box">
              <view class="demo-bead-mirror"></view>
            </view>
            <text class="help-text">单击珠子镜像翻转</text>
          </view>
          <view class="help-item">
            <view class="help-icon-box">
              <view class="demo-toolbar">
                <view class="dt-dot"></view>
                <view class="dt-dot active"></view>
                <view class="dt-dot"></view>
              </view>
            </view>
            <text class="help-text">工具栏更多功能</text>
          </view>
          <view class="help-item">
            <view class="help-icon-box">
              <view class="demo-pinch">
                <view class="finger finger-1"></view>
                <view class="finger finger-2"></view>
              </view>
            </view>
            <text class="help-text">双指捏合缩放画布</text>
          </view>
        </view>
        <view class="help-footer">
          <view class="checkbox-row" @click="toggleDontShowHelp">
            <view class="checkbox" :class="{checked: dontShowHelpAgain}"></view>
            <text class="checkbox-label">下次不再弹出</text>
          </view>
          <button class="help-btn" @click="closeHelpPopup">我知道了</button>
        </view>
      </view>
    </view>

    <canvas type="2d" id="exportCanvas" class="export-canvas" style="width: 560rpx; height: 560rpx; position: fixed; left: 200%; top: 0; pointer-events: none; opacity: 0;"></canvas>
  </view>
</template>

<script setup>
import { onShow } from '@dcloudio/uni-app'
import { computed, getCurrentInstance, nextTick, onMounted, ref, watch } from 'vue'
// 直接从 api.js 导入以避免 index.js 可能的重导出问题
import {
addToCart,
designCategoryList,
designProductList,
uploadFile
} from '../../api/api.js'
import { updateCartBadgeNow } from '../../utils/cartBadge.js'
import { resolveImageUrl } from '../../utils/imageHelper.js'

// 实例
const instance = getCurrentInstance()

const isMounted = ref(false)
const didInit = ref(false)
const logoPath = '/static/logo/final_logo.jpg'
console.log('Logo Path set to absolute:', logoPath)

// 数据状态
const categories = ref([])
const colorSeries = ref([])
const colorSeriesAll = ref([])
const activeCategory = ref('main_bead')
const activeColor = ref('')
const goods = ref([])
const allGoods = ref([]) // 用于前端分页的完整数据缓存
const loading = ref(false)

// 搜索相关
const searchKeyword = ref('')
const searchResults = ref([])
const showSearchResults = ref(false)
const searchTimer = ref(null)
// 分页状态
const page = ref(1)
const pageSize = ref(20)
const totalPages = ref(1)
const productScrollTop = ref(0)

const beads = ref([])
const clickedId = ref(null)

// 尺寸
const selectedSize = ref(Number(uni.getStorageSync('diy_selected_size')) || 16)
watch(selectedSize, (newVal) => {
  uni.setStorageSync('diy_selected_size', newVal)
})
const sizeOptions = [13, 14, 15, 16, 17, 18, 19, 20, 21, 22]
const doubleSizeOptions = [26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44]
const showSizeSelector = ref(false)

// 戴法选择：single-单圈，double-双圈
const wearStyle = ref(uni.getStorageSync('diy_wear_style') || 'single')
watch(wearStyle, (newVal) => {
  uni.setStorageSync('diy_wear_style', newVal)
})

// 根据戴法获取尺寸选项
const currentSizeOptions = computed(() => {
  return wearStyle.value === 'double' ? doubleSizeOptions : sizeOptions
})

// 拖拽和交换
const draggingIndex = ref(-1)
const isDragging = ref(false)
const swapMode = ref(false)
const firstSwapIndex = ref(-1)
const isAutoArranged = ref(false) // 是否一键排列（均匀分布）

// 弹窗
const showOrderPreview = ref(false)
const showInitGuide = ref(false)
const showHelpPopup = ref(false)
const dontShowHelpAgain = ref(false)
const isToolbarCollapsed = ref(false)

// 飞行动画
const flyingBead = ref(null)
const flyingBeadStyle = ref('')

let beadIdCounter = 0
let productRequestSeq = 0
let suppressScrollToLowerUntil = 0
let pendingAnimTimer = null
let pendingAddTimer = null
let pendingNewFlagTimer = null

function resetProductListView() {
  productScrollTop.value = 0
  suppressScrollToLowerUntil = Date.now() + 600
}

function cancelPendingAdd() {
  let didCancel = false
  if (pendingAnimTimer) {
    clearTimeout(pendingAnimTimer)
    pendingAnimTimer = null
    didCancel = true
  }
  if (pendingAddTimer) {
    clearTimeout(pendingAddTimer)
    pendingAddTimer = null
    didCancel = true
  }
  if (pendingNewFlagTimer) {
    clearTimeout(pendingNewFlagTimer)
    pendingNewFlagTimer = null
  }
  if (flyingBead.value) {
    flyingBead.value = null
    flyingBeadStyle.value = ''
    // 视觉元素的清理不应视为拦截了“数据入库”，
    // 只有 pendingAddTimer/pendingAnimTimer 才是真正的数据关卡。
    // 如果没有 timer 但有 flyingBead，说明数据已经入库（timer已执行），此时撤销应该执行 pop。
  }
  clickedId.value = null
  if (beads.value.length) {
    const last = beads.value[beads.value.length - 1]
    if (last && last.isNew) last.isNew = false
  }
  return didCancel
}

// 过滤绳子分类
const displayCategories = computed(() => categories.value.filter(c => c.keyCode !== 'rope'))

// 获取当前选中主分类的子分类列表
const currentSubCategories = computed(() => {
  const current = categories.value.find(c => c.keyCode === activeCategory.value)
  return current && Array.isArray(current.children) ? current.children : []
})

function getSubCategoryKey(item) {
  if (!item) return ''
  // 优先使用 keyCode，其次 id/key/code/value/name
  const v = item.keyCode ?? item.id ?? item.key ?? item.code ?? item.value ?? item.name
  return normalizeFilterValue(v)
}

function getColorItemKey(item) {
  return getSubCategoryKey(item)
}

function getClassificationItemKey(item) {
  return getSubCategoryKey(item)
}

function normalizeOptionItem(item) {
  if (item === undefined || item === null) return null
  if (typeof item === 'string' || typeof item === 'number') {
    const v = String(item)
    return { id: v, name: v }
  }
  if (typeof item === 'object') {
    const id = normalizeFilterValue(item.id ?? item.keyCode ?? item.key ?? item.code ?? item.value ?? item.name ?? item.title ?? item.label)
    const name = String(item.name ?? item.title ?? item.label ?? item.value ?? item.keyCode ?? item.id ?? id)
    return { ...item, id: id || name, name }
  }
  return null
}

function normalizeOptionList(list) {
  if (!Array.isArray(list)) return []
  return list.map(normalizeOptionItem).filter(Boolean)
}

function normalizeFilterValue(v) {
  if (v === undefined || v === null || v === '') return ''
  return String(v)
}

function isPendant(bead) {
  const title = String(bead?.title || '')
  return title.endsWith('吊坠')
}

// 目标珠子数
const targetCount = computed(() => {
  if (!beads.value.length) return Math.round(selectedSize.value * 10 / 8)
  const avg = beads.value.reduce((s, b) => s + Number(b.size || 8), 0) / beads.value.length
  return Math.round(selectedSize.value * 10 / avg)
})

// 总价
const totalPrice = computed(() => beads.value.reduce((s, b) => s + Number(b.price || 0), 0).toFixed(2))

// 重量
const weightG = computed(() => {
  return beads.value.reduce((s, b) => {
    const sz = Number(b.size || 8)
    return s + (sz >= 10 ? 0.6 : sz >= 8 ? 0.4 : 0.3)
  }, 0).toFixed(1)
})

// 订单项
const orderItems = computed(() => {
  const map = {}
  beads.value.forEach(b => {
    const k = `${b.title}_${b.price}`
    if (!map[k]) map[k] = { 
      productId: b.productId,
      materialId: b.productId, // Compatible with backend expectations
      id: b.productId, // Compatible with backend expectations
      title: b.title, 
      price: b.price, 
      quantity: 0,
      imageUrl: b.imageUrl,
      color: b.color 
    }
    map[k].quantity++
  })
  return Object.values(map)
})

function getBeadArcSizeRpx(bead) {
  // 吊坠占一小部分空间 (例如 3mm)，避免重叠
  if (isPendant(bead)) return 3 * 7
  
  const base = Number(bead?.size || 8) * 7
  return base
}

const maxRadiusHistory = ref(0)
const manualScale = ref(1.0) // 手动缩放比例
const initialPinchDistance = ref(0) // 初始双指距离
const startManualScale = ref(1.0) // 缩放开始时的比例

// 动态计算半径
const visualRadius = computed(() => {
  // 严格按照表格规格计算
  // 表格数据显示，绳长大约为 (手围 + 2.4cm)
  // 例如 16cm 手围，8mm 珠子 23 颗 = 184mm，即 18.4cm
  const circumferenceMm = (selectedSize.value + 2.4) * 10
  const circumferenceRpx = circumferenceMm * 7
  return circumferenceRpx / (2 * Math.PI)
})

const rawLayoutRadius = computed(() => {
  const count = beads.value.length
  if (!count) return visualRadius.value
  const totalArc = beads.value.reduce((s, b) => s + getBeadArcSizeRpx(b), 0)
  const minR = totalArc / (2 * Math.PI)
  return Math.max(visualRadius.value, minR)
})

const layoutRadius = computed(() => {
  return Math.max(rawLayoutRadius.value, maxRadiusHistory.value)
})

watch(rawLayoutRadius, (val) => {
  if (val > maxRadiusHistory.value) {
    maxRadiusHistory.value = val
  }
})

watch(selectedSize, () => {
  maxRadiusHistory.value = 0
})

// 动态缩放画布，确保大尺寸也能完整显示
const canvasScale = computed(() => {
  const maxR = 250 // 最大安全半径 (留出30rpx边距)
  const maxBeadSize = beads.value.reduce((m, b) => Math.max(m, getBeadArcSizeRpx(b)), 0)
  const safeR = layoutRadius.value + maxBeadSize / 2
  
  let autoScale = 1
  if (safeR > maxR) {
    autoScale = maxR / safeR
  }
  
  // 结合手动缩放
  return autoScale * manualScale.value
})

// 计算珠子布局
const beadLayouts = computed(() => {
  const count = beads.value.length
  if (!count) return []
  
  const radius = layoutRadius.value
  const layouts = []
  
  // 计算所有珠子的总弧长（不含间隙）
  let totalBeadArc = 0
  beads.value.forEach(b => {
    const s = getBeadArcSizeRpx(b)
    totalBeadArc += s
  })
  
  // 计算圆周长
  const circumference = 2 * Math.PI * radius
  
  // 确定间隙
  let gap = 0 // 默认间隙设为0，由 visualRadius 保证不重叠
  let startAngle = -Math.PI / 2 // 默认从顶部开始
  
  if (isAutoArranged.value) {
    // 均匀分布模式
    const remainingArc = circumference - totalBeadArc
    
    // 计算有效间隙数量：只有 珠子-珠子 之间才分配间隙
    // 吊坠前后都紧贴，不分配间隙
    let validGapCount = 0
    
    if (count > 0) {
        for (let i = 0; i < count; i++) {
            const curr = beads.value[i]
            const next = beads.value[(i + 1) % count]
            // 如果当前和下一个都不是吊坠，则需要分配间隙
            if (!isPendant(curr) && !isPendant(next)) {
                validGapCount++
            }
        }
    }
    
    if (validGapCount > 0) {
        gap = remainingArc / validGapCount
    } else {
        gap = 0
    }
  }
  
  // 布局计算
  let currentAngle = startAngle
  
  for (let i = 0; i < count; i++) {
    if (i === 0) {
      layouts.push({ angle: currentAngle })
    } else {
      const prevBead = beads.value[i-1]
      const currBead = beads.value[i]
      const prevSize = getBeadArcSizeRpx(prevBead)
      const currSize = getBeadArcSizeRpx(currBead)
      
      // 判断是否需要添加间隙
      // 只有当前一个不是吊坠，且当前也不是吊坠时，才应用 gap
      const applyGap = (!isPendant(prevBead) && !isPendant(currBead)) ? gap : 0

      // 累加角度
      // 使用弧长计算角度：arc = r1 + r2 + gap
      const arc = (prevSize / 2) + (currSize / 2) + applyGap
      const theta = arc / radius
      currentAngle += theta
      layouts.push({ angle: currentAngle })
    }
  }
  
  return layouts
})

// 检查容量（防止重叠超过一圈）
function checkCapacity(newItem) {
  // 最大周长 (mm) - 严格按照表格规格 (手围 + 2.4cm)
  const maxMm = (selectedSize.value + 2.4) * 10
  
  // 当前总长 (mm)
  let currentMm = 0
  beads.value.forEach(b => {
    // 吊坠也占长度 (3mm)
    if (isPendant(b)) {
      currentMm += 3
    } else {
      currentMm += Number(b.size || 8)
    }
  })
  
  // 新增的珠子
  let addSize = 0
  if (isPendant(newItem)) {
    addSize = 3
  } else {
    addSize = Number(newItem.size || 8)
  }
  
  // 允许一定的误差（例如 25mm，约3颗珠子），防止因历史数据轻微超载导致无法操作
  // 只要不是恶意堆叠，稍微超一点点是可以接受的
  if (currentMm + addSize > maxMm + 25) {
    // 调试用：打印详细容量信息
    console.log(`[Capacity Check Failed] Current: ${currentMm}mm, Adding: ${addSize}mm, Total: ${currentMm + addSize}mm`)
    console.log(`[Capacity Limit] SelectedSize: ${selectedSize.value}, MaxMm: ${maxMm}, Threshold: ${maxMm + 25}`)
    return false
  }
  console.log(`[Capacity Check Passed] Total: ${currentMm + addSize}mm <= Threshold: ${maxMm + 25}mm`)
  return true
}

// 珠子样式 - 适应动态半径
function getBeadStyle(bead, index) {
  const count = beads.value.length
  if (!count) return ''
  
  const layout = beadLayouts.value[index]
  if (!layout) return ''
  
  const size = Number(bead.size || 8) * 7
  const radius = layoutRadius.value
  const angle = layout.angle
  
  // 280 是画布中心 (560/2)
  const pendant = isPendant(bead)
  const placeRadius = pendant ? (radius + size / 2) : radius
  const cx = 280 + placeRadius * Math.cos(angle) - size / 2
  const cy = 280 + placeRadius * Math.sin(angle) - size / 2
  
  const rotationDeg = pendant
    ? (angle - Math.PI / 2) * (180 / Math.PI)
    : (angle + Math.PI / 2) * (180 / Math.PI)
  
  let color = bead.color || '#e8e8e8'
  if (color.includes('gradient')) {
    const m = color.match(/#[0-9a-fA-F]{6}/)
    if (m) color = m[0]
  }

  // 如果有图片且未加载失败，背景设为透明
  if (bead.imageUrl && !bead.loadFailed) {
    color = 'transparent'
  }
  
  const scaleX = bead.mirrored ? -1 : 1
  const scale = draggingIndex.value === index ? 1.15 : 1
  
  // 大尺寸材料放在底层 (z-index < 绳子的5)
  // 普通材料放在顶层 (z-index > 绳子的5)
  // 阈值：24mm * 7 = 168rpx
  const zIndex = size >= 168 ? 1 : 10
  
  return `left:${cx}rpx;top:${cy}rpx;width:${size}rpx;height:${size}rpx;background:${color};transform:rotate(${rotationDeg}deg) scaleX(${scaleX}) scale(${scale});z-index:${zIndex};`
}

// 切换排列模式
function toggleAutoArrange() {
  if (!beads.value.length) return
  vibrate()
  isAutoArranged.value = !isAutoArranged.value
  uni.showToast({ 
    title: isAutoArranged.value ? '已均匀排列' : '已紧凑排列', 
    icon: 'none' 
  })
}

function toggleToolbar() {
  vibrate()
  isToolbarCollapsed.value = !isToolbarCollapsed.value
}

function onToolbarContainerClick() {
  if (isToolbarCollapsed.value) toggleToolbar()
}

// 添加珠子 - 带飞行动画
function addBead(g, event) {
  if (g.stock === 0) {
    uni.showToast({ title: '暂时缺货', icon: 'none' })
    return
  }
  // 移除 cancelPendingAdd() 调用，允许快速连续点击添加多个珠子
  // cancelPendingAdd()
  // 移除硬性数量限制，由 checkCapacity 根据手围和珠径动态控制
  // if (beads.value.length >= 32) { ... }

  // 检查是否会重合（超过一圈）
  if (!checkCapacity(g)) {
    uni.showToast({ title: '已达到当前手围的最大容量', icon: 'none' })
    return
  }

  // 立即添加珠子到数据（不等待动画）
  // 扣减库存
  if (g.stock > 0) {
    g.stock--
  }

  const newBead = {
    _id: `b_${++beadIdCounter}`,
    productId: g.id,
    title: g.title,
    price: g.price,
    size: g.size,
    color: g.color,
    imageUrl: g.imageUrl,
    loadFailed: false,
    isNew: true
  }
  beads.value.push(newBead)

  // 设置点击反馈效果
  clickedId.value = g.id
  vibrate()

  // 清除之前的点击反馈定时器
  if (pendingNewFlagTimer) {
    clearTimeout(pendingNewFlagTimer)
  }

  // 移除 isNew 标记
  pendingNewFlagTimer = setTimeout(() => {
    pendingNewFlagTimer = null
    newBead.isNew = false
    clickedId.value = null
  }, 350)

  // 飞行动画（可选，不影响数据添加）
  // 使用局部变量保存当前珠子的信息，避免快速点击时动画混乱
  const beadColor = g.color || '#e8e8e8'
  const beadImageUrl = g.imageUrl

  // 获取颜色
  let color = beadColor
  if (color.includes('gradient')) {
    const m = color.match(/#[0-9a-fA-F]{6}/)
    if (m) color = m[0]
  }

  // 启动飞行动画
  flyingBead.value = { color, imageUrl: beadImageUrl }

  // 计算起点位置
  let startX = '50%'
  let startY = '100%'

  if (event && event.detail) {
    // 尝试获取点击位置
    // 注意：event.detail 在不同平台可能包含 x,y 或 clientX,clientY
    const x = event.detail.x || event.touches?.[0]?.clientX || 0
    const y = event.detail.y || event.touches?.[0]?.clientY || 0
    if (x && y) {
      startX = x + 'px'
      startY = y + 'px'
    }
  } else if (event && event.touches && event.touches[0]) {
    startX = event.touches[0].clientX + 'px'
    startY = event.touches[0].clientY + 'px'
  }

  // 动态创建动画关键帧
  // 由于 uniapp 无法动态修改 @keyframes，我们使用 CSS 变量或动态 style 结合 transition
  // 但为了效果更好，我们使用 transform 直接定位起始点

  flyingBeadStyle.value = `
    left: ${startX};
    top: ${startY};
    background: ${color};
    transform: translate(-50%, -50%) scale(0.5);
    opacity: 1;
    transition: all 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  `

  // 强制重绘后执行动画
  pendingAnimTimer = setTimeout(() => {
    pendingAnimTimer = null
    // 飞向中心 (屏幕中心 50vw, 50vh 附近，或者具体画布中心)
    // 画布中心大概在 header 下方，main-area 中间
    // 简单起见，飞向屏幕中心
    flyingBeadStyle.value = `
      left: 50%;
      top: 40%;
      background: ${color};
      transform: translate(-50%, -50%) scale(1);
      opacity: 0;
      transition: all 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    `
  }, 20)

  // 动画结束后清理
  pendingAddTimer = setTimeout(() => {
    pendingAddTimer = null
    flyingBead.value = null
  }, 520)
}

function updateStock(productId, delta) {
  const g = goods.value.find(x => x.id === productId)
  if (g) g.stock += delta
  
  // 同时更新 allGoods 中的库存（如果存在）
  const ag = allGoods.value.find(x => x.id === productId)
  if (ag && ag !== g) ag.stock += delta
}

function onBeadImageError(bead, index) {
  console.error('Bead image load failed:', bead.imageUrl)
  bead.loadFailed = true
}

// 点击珠子
function onBeadTap(i) {
  if (isDragging.value) return
  
  // 如果在交换模式
  if (swapMode.value) {
    if (firstSwapIndex.value === i) {
      // 点击同一颗，取消交换模式
      swapMode.value = false
      firstSwapIndex.value = -1
      uni.showToast({ title: '已取消', icon: 'none', duration: 800 })
    } else {
      // 交换两颗珠子
      swapBeads(firstSwapIndex.value, i)
      swapMode.value = false
      firstSwapIndex.value = -1
      uni.showToast({ title: '已交换位置', icon: 'success', duration: 800 })
    }
    return
  }
  
  // 正常点击，显示操作菜单
  const bead = beads.value[i]
  const mirrorText = bead.mirrored ? '取消镜像' : '镜像翻转'
  uni.showActionSheet({
    itemList: ['移除此珠', '调整位置', mirrorText],
    success: (res) => {
      if (res.tapIndex === 0) {
        // 移除
        vibrate()
        const removed = beads.value[i]
        beads.value.splice(i, 1)
        if (removed && removed.productId) {
          updateStock(removed.productId, 1)
        }
        uni.showToast({ title: '已移除', icon: 'none', duration: 800 })
      } else if (res.tapIndex === 1) {
        // 进入交换模式
        swapMode.value = true
        firstSwapIndex.value = i
        uni.showToast({ title: '点击另一颗珠子交换位置', icon: 'none', duration: 1500 })
      } else if (res.tapIndex === 2) {
        // 镜像翻转
        toggleBeadMirror(i)
        uni.showToast({ title: bead.mirrored ? '已取消镜像' : '已镜像', icon: 'none', duration: 800 })
      }
    }
  })
}

// 长按拖拽
const dragStartPos = ref({ x: 0, y: 0 })
const dragTargetIndex = ref(-1)
const canvasRect = ref(null)

function startDrag(i) {
  if (beads.value.length < 2) {
    uni.showToast({ title: '至少需要2颗珠子才能调整', icon: 'none' })
    return
  }
  isDragging.value = true
  draggingIndex.value = i
  dragTargetIndex.value = -1
  vibrate()
  uni.showToast({ title: '拖动到目标位置松手', icon: 'none', duration: 1500 })
}

function onTouchStart(e) {
  // 总是更新画布位置，确保后续计算准确
  const query = uni.createSelectorQuery().in(instance)
  query.select('.canvas').boundingClientRect(data => {
    if (data) canvasRect.value = data
  }).exec()
  
  // 双指缩放
  if (e.touches.length === 2) {
    const t1 = e.touches[0]
    const t2 = e.touches[1]
    const dx = t2.clientX - t1.clientX
    const dy = t2.clientY - t1.clientY
    initialPinchDistance.value = Math.sqrt(dx * dx + dy * dy)
    startManualScale.value = manualScale.value
    return
  }
  
  if (isDragging.value) {
    const touch = e.touches[0]
    if (touch) {
      dragStartPos.value = { x: touch.clientX, y: touch.clientY }
    }
  }
}

function onTouchMove(e) {
  // 双指缩放
  if (e.touches.length === 2 && initialPinchDistance.value > 0) {
    const t1 = e.touches[0]
    const t2 = e.touches[1]
    const dx = t2.clientX - t1.clientX
    const dy = t2.clientY - t1.clientY
    const currentDist = Math.sqrt(dx * dx + dy * dy)
    
    const scaleRatio = currentDist / initialPinchDistance.value
    let newScale = startManualScale.value * scaleRatio
    
    // 限制缩放范围 (例如 0.5 到 3.0)
    if (newScale < 0.5) newScale = 0.5
    if (newScale > 3.0) newScale = 3.0
    
    manualScale.value = newScale
    return
  }

  if (!isDragging.value || draggingIndex.value < 0) return
  
  const touch = e.touches[0]
  if (!touch) return
  
  const count = beads.value.length
  if (count < 2) return
  
  // 优先使用角度计算（更符合圆环直觉）
  if (canvasRect.value) {
    const rect = canvasRect.value
    const centerX = rect.left + rect.width / 2
    const centerY = rect.top + rect.height / 2
    
    const dx = touch.clientX - centerX
    const dy = touch.clientY - centerY
    
    // 计算角度 (-PI 到 PI)
    const angle = Math.atan2(dy, dx)
    
    // 转换为 0-2PI，起点在上方 (-PI/2)
    // -PI/2 -> 0
    let normalizedAngle = angle + Math.PI / 2
    if (normalizedAngle < 0) normalizedAngle += 2 * Math.PI
    
    let targetIndex = -1
    let minDiff = Infinity
    
    // 遍历所有布局位置，寻找角度最接近的
    beadLayouts.value.forEach((layout, i) => {
      // 将布局角度转换为 0-2PI
      let layoutNorm = layout.angle + Math.PI / 2
      layoutNorm = layoutNorm % (2 * Math.PI)
      if (layoutNorm < 0) layoutNorm += 2 * Math.PI
      
      let diff = Math.abs(layoutNorm - normalizedAngle)
      // 处理圆环边界 (0 和 2PI 是重合的)
      if (diff > Math.PI) diff = 2 * Math.PI - diff
      
      if (diff < minDiff) {
        minDiff = diff
        targetIndex = i
      }
    })
    
    if (targetIndex >= 0 && targetIndex < count && targetIndex !== draggingIndex.value) {
      swapBeads(draggingIndex.value, targetIndex)
      draggingIndex.value = targetIndex
      vibrate()
    }
    return
  }
  
  // 降级逻辑 (如果获取不到canvasRect)
  const dx = touch.clientX - dragStartPos.value.x
  const dy = touch.clientY - dragStartPos.value.y
  const distance = Math.sqrt(dx * dx + dy * dy)
  
  if (distance > 30) {
    const angle = Math.atan2(dy, dx)
    let newTarget
    if (dx > 40) {
      newTarget = (draggingIndex.value + 1) % count
    } else if (dx < -40) {
      newTarget = (draggingIndex.value - 1 + count) % count
    }
    
    if (newTarget !== undefined && newTarget !== draggingIndex.value) {
      swapBeads(draggingIndex.value, newTarget)
      draggingIndex.value = newTarget
      dragStartPos.value = { x: touch.clientX, y: touch.clientY }
      vibrate()
    }
  }
}

function onTouchEnd(e) {
  // 如果是双指操作结束
  if (e.touches.length < 2) {
    initialPinchDistance.value = 0
  }

  if (isDragging.value) {
    isDragging.value = false
    draggingIndex.value = -1
    dragTargetIndex.value = -1
  }
}

// 辅助功能
function swapBeads(i, j) {
  const temp = beads.value[i]
  beads.value[i] = beads.value[j]
  beads.value[j] = temp
}

function toggleBeadMirror(i) {
  const bead = beads.value[i]
  bead.mirrored = !bead.mirrored
}

function mirrorAllBeads() {
  if (!beads.value.length) return
  vibrate()
  const anyMirrored = beads.value.some(b => b.mirrored)
  beads.value.forEach(b => b.mirrored = !anyMirrored)
  uni.showToast({ 
    title: anyMirrored ? '已全部取消镜像' : '已全部镜像', 
    icon: 'none' 
  })
}

function undoLastBead() {
  const didCancel = cancelPendingAdd()
  // 如果确实取消了正在进行的添加操作（如动画或等待入库），则不进行 pop
  if (didCancel) {
    console.log('Undo: Cancelled pending add (timer running)')
    return
  }
  
  if (!beads.value.length) return
  vibrate()
  const removed = beads.value.pop()
  console.log('Undo: Popped bead. Remaining:', beads.value.length)
  if (removed && removed.productId) {
    updateStock(removed.productId, 1)
  }
}

function confirmClear() {
  cancelPendingAdd()
  if (!beads.value.length) return
  uni.showModal({
    title: '确认清空',
    content: '确定要清空所有珠子吗？',
    success: (res) => {
      if (res.confirm) {
        // 恢复所有库存
        beads.value.forEach(b => {
          if (b.productId) updateStock(b.productId, 1)
        })
        beads.value = []
        maxRadiusHistory.value = 0 // 重置绳子记忆半径
        vibrate()
      }
    }
  })
}

// 切换不再提示
function toggleDontShowHelp() {
  dontShowHelpAgain.value = !dontShowHelpAgain.value
  // 实时更新缓存状态
  if (dontShowHelpAgain.value) {
    uni.setStorageSync('dontShowHelp', 'true')
  } else {
    uni.removeStorageSync('dontShowHelp')
  }
}

// 关闭帮助弹窗
function closeHelpPopup() {
  showHelpPopup.value = false
  // 缓存逻辑已移至 toggleDontShowHelp 实时处理
}

// 显示提示
function showTips() {
  // 打开时同步缓存状态
  const hide = uni.getStorageSync('dontShowHelp')
  dontShowHelpAgain.value = !!hide
  showHelpPopup.value = true
}

function vibrate() {
  uni.vibrateShort()
}

// API交互
async function initData() {
  try {
    const catRes = await designCategoryList()
    // 确保 categories 是数组
    // 接口现在返回 [{ keyCode, name, children: [] }]
    categories.value = Array.isArray(catRes) ? catRes : (catRes.data || [])
    
    // 清空旧数据
    colorSeries.value = []
    colorSeriesAll.value = []
    
    if (categories.value.length > 0) {
      // 默认选中第一个非绳子分类
      // 优先选中 'main_bead'，如果没有则选中第一个非rope
      const mainBead = categories.value.find(c => c.keyCode === 'main_bead')
      const firstValid = categories.value.find(c => c.keyCode !== 'rope')
      
      if (mainBead) {
        activeCategory.value = mainBead.keyCode
      } else if (firstValid) {
        activeCategory.value = firstValid.keyCode
      } else {
        activeCategory.value = categories.value[0].keyCode
      }
    }
    loadProducts()
  } catch (e) {
    console.error('初始化设计台数据失败:', e)
  }
}

function ensureInit() {
  // 每次显示页面时检查是否需要弹出操作指南
  try {
    const hide = uni.getStorageSync('dontShowHelp')
    if (!hide && !showHelpPopup.value) {
      setTimeout(() => {
        // 再次检查，避免在延时期间用户已经离开或手动打开
        if (!showHelpPopup.value) {
            showHelpPopup.value = true
        }
      }, 500)
    }
  } catch (e) {
    console.error('Storage error:', e)
  }

  if (didInit.value) return
  didInit.value = true

  nextTick(() => {
    initData()
  })
}

// 切换分类
function switchCategory(key) {
  if (activeCategory.value === key) return
  activeCategory.value = key
  activeColor.value = '' // 切换分类时重置子分类
  
  // 重置状态
  resetProductListView()
  page.value = 1
  totalPages.value = 1
  goods.value = []
  allGoods.value = []
  loadProducts()
}

// 切换子分类 (原 switchColor)
function switchColor(key) {
  const normalizedKey = normalizeFilterValue(key)
  if (normalizedKey === '') {
    activeColor.value = ''
  } else if (activeColor.value === normalizedKey) {
    // 再次点击取消选中? 还是保持? 
    // 通常点击已选中的可以取消，或者不做操作。这里保持 toggle 逻辑
    activeColor.value = '' // 取消选中
  } else {
    activeColor.value = normalizedKey
  }

  resetProductListView()
  page.value = 1
  totalPages.value = 1
  goods.value = []
  allGoods.value = []
  loadProducts()
}

// 废弃的方法，保留空实现以防模板报错
function switchClassification(key, id) {}

// ==================== 搜索相关方法 ====================

// 搜索输入处理（防抖）
function onSearchInput() {
  if (searchTimer.value) {
    clearTimeout(searchTimer.value)
  }
  searchTimer.value = setTimeout(() => {
    performSearch()
  }, 300)
}

// 执行搜索
async function performSearch() {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    searchResults.value = []
    showSearchResults.value = false
    return
  }

  // 从所有分类中搜索商品
  const results = []
  const lowerKeyword = keyword.toLowerCase()

  // 遍历所有分类获取商品
  for (const category of categories.value) {
    if (category.keyCode === 'rope') continue // 跳过绳子分类

    try {
      const res = await designProductList({
        categories: [category.keyCode],
        page: 1,
        size: 100 // 获取较多数据用于搜索
      })

      // 解析返回数据，与 loadProducts 保持一致
      const products = Array.isArray(res) ? res : []
      for (const product of products) {
        // 处理图片路径
        let imageUrl = product.imageUrl || product.image || ''
        imageUrl = resolveImageUrl(imageUrl)
        if (imageUrl && !imageUrl.startsWith('data:')) {
          try {
            imageUrl = encodeURI(imageUrl)
          } catch (e) {}
        }

        if (product.title && product.title.toLowerCase().includes(lowerKeyword)) {
          results.push({
            ...product,
            imageUrl,
            categoryKey: category.keyCode,
            categoryName: category.name
          })
        }
      }
    } catch (e) {
      console.error('搜索分类失败:', category.keyCode, e)
    }
  }

  searchResults.value = results.slice(0, 20) // 最多显示20条
  showSearchResults.value = true
}

// 搜索框获得焦点
function onSearchFocus() {
  if (searchKeyword.value.trim() && searchResults.value.length > 0) {
    showSearchResults.value = true
  }
}

// 搜索框失去焦点（延迟隐藏，以便点击下拉项）
function onSearchBlur() {
  setTimeout(() => {
    showSearchResults.value = false
  }, 200)
}

// 清空搜索
// 选择搜索结果
async function selectSearchResult(item) {
  // 1. 切换到对应分类
  if (activeCategory.value !== item.categoryKey) {
    activeCategory.value = item.categoryKey
  }
  
  // 2. 重置子分类筛选
  activeColor.value = ''

  // 3. 只显示选中的商品，过滤掉其他商品
  resetProductListView()
  page.value = 1
  totalPages.value = 1
  
  // 设置商品列表只包含选中的商品
  goods.value = [{
    ...item,
    loaded: false
  }]
  allGoods.value = []

  // 4. 隐藏搜索结果
  showSearchResults.value = false
  
  // 5. 高亮显示该商品
  nextTick(() => {
    clickedId.value = item.id
    setTimeout(() => {
      clickedId.value = null
    }, 1000)
  })
}

// 清空搜索，恢复显示所有商品
function clearSearch() {
  searchKeyword.value = ''
  searchResults.value = []
  showSearchResults.value = false
  if (searchTimer.value) {
    clearTimeout(searchTimer.value)
    searchTimer.value = null
  }
  
  // 重新加载当前分类的所有商品
  resetProductListView()
  page.value = 1
  totalPages.value = 1
  goods.value = []
  allGoods.value = []
  loadProducts()
}

// 加载商品
async function loadProducts(isLoadMore = false) {
  const requestSeq = ++productRequestSeq
  
  // 如果是加载更多且正在使用前端分页
  if (isLoadMore && allGoods.value.length > 0) {
      const start = (page.value - 1) * pageSize.value
      const end = start + pageSize.value
      const nextBatch = allGoods.value.slice(start, end)
      if (nextBatch.length) {
          goods.value.push(...nextBatch)
      }
      loading.value = false
      return
  }

  if (isLoadMore && page.value > totalPages.value) return

  loading.value = true
  try {
    const params = {
      categories: activeCategory.value ? [activeCategory.value] : [],
      colorSeries: activeColor.value ? [activeColor.value] : [],
      page: page.value,
      size: pageSize.value
    }
    
    // 不再传递 classificationDetailKey1-8，因为 API 已经整合

    const res = await designProductList(params)
    if (requestSeq !== productRequestSeq) return
    
    // 处理返回数据
    // 预期 res 是一个数组(materials) 或者包含 materials 的对象
    // api.js 的 getDiyMaterialList 已经处理了大部分解构逻辑，返回的是 materials 数组 (带有 totalPages 属性)
    
    const list = Array.isArray(res) ? res : []
    const total = res.totalPages || 0
    
    const processedList = list.map(item => {
      // 确保图片路径完整
      let imageUrl = item.imageUrl || item.image || ''
      imageUrl = resolveImageUrl(imageUrl)
      
      // 尝试对中文路径进行编码
      if (imageUrl && !imageUrl.startsWith('data:')) {
        try {
          imageUrl = encodeURI(imageUrl)
        } catch (e) {}
      }
      
      // 确保有 color 字段
      if (!item.color) item.color = '#f5f5f5'
      return { ...item, imageUrl, loaded: false }
    })

    if (isLoadMore) {
      goods.value = [...goods.value, ...processedList]
      if (total) totalPages.value = total
    } else {
      // 第一次加载
      // 检查是否需要前端分页 (如果后端没分页但返回了大量数据)
      if (total === 0 && processedList.length > pageSize.value) {
          allGoods.value = processedList
          totalPages.value = Math.ceil(processedList.length / pageSize.value)
          goods.value = processedList.slice(0, pageSize.value)
      } else {
          allGoods.value = []
          goods.value = processedList
          if (total) {
            totalPages.value = total
          } else {
             // 估算
             totalPages.value = processedList.length < pageSize.value ? page.value : page.value + 1
          }
      }
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function onProductScrollToLower() {
  if (Date.now() < suppressScrollToLowerUntil) return
  if (page.value < totalPages.value && !loading.value) {
    page.value++
    loadProducts(true)
  }
}

// 戴法选择
function selectWearStyle(style) {
  wearStyle.value = style
  uni.setStorageSync('diy_wear_style', style)
  // 切换戴法时，如果当前尺寸不在新范围内，自动选择范围内的第一个
  const options = style === 'double' ? doubleSizeOptions : sizeOptions
  if (!options.includes(selectedSize.value)) {
    selectedSize.value = options[0]
    uni.setStorageSync('diy_selected_size', options[0])
  }
}

// 尺寸选择
function selectSize(s) {
  selectedSize.value = s
  uni.setStorageSync('diy_selected_size', s)
}

function confirmSize() {
  showSizeSelector.value = false
  showInitGuide.value = false
}

function showPreview() {
  if (!beads.value.length) return
  showOrderPreview.value = true
}

function startDesign() {
  confirmSize()
}

// 生成设计图 (Canvas 2D)
async function generateDesignImage() {
  uni.showLoading({ title: '生成设计图中...', mask: true })
  
  try {
    // 1. 获取 Canvas 节点
    const { canvas, width: canvasWidth, height: canvasHeight } = await new Promise((resolve, reject) => {
      const query = uni.createSelectorQuery().in(instance)
      query.select('#exportCanvas')
        .fields({ node: true, size: true })
        .exec((res) => {
          if (res[0] && res[0].node) {
            resolve({
                canvas: res[0].node,
                width: res[0].width,
                height: res[0].height
            })
          } else {
            // reject(new Error('未找到 Canvas 节点'))
            // 降级处理：如果没有Canvas（可能被暂时屏蔽），则跳过生成
            console.warn('Canvas not found or disabled')
            resolve({ canvas: null })
          }
        })
    })

    if (!canvas) {
        // 返回 Logo 路径作为替补，确保流程走通
        return logoPath
    }

    const ctx = canvas.getContext('2d')
    
    // 获取设备像素比和窗口信息
    let dpr = 2
    try {
        const info = uni.getWindowInfo ? uni.getWindowInfo() : uni.getSystemInfoSync()
        dpr = info.pixelRatio || 2
    } catch (e) {
        console.warn('获取系统信息失败', e)
    }
    
    // 设置画布物理尺寸 (解决模糊问题)
    canvas.width = canvasWidth * dpr
    canvas.height = canvasHeight * dpr
    
    // 缩放上下文，使得后续绘图可以使用逻辑像素
    ctx.scale(dpr, dpr)
    
    // rpx to px 比例 (基于 750 设计稿)
    const ratio = canvasWidth / 560 // 560rpx是CSS设置的宽度
    const r2p = (rpx) => rpx * ratio
    
    // 逻辑宽高
    const width = canvasWidth
    const height = canvasHeight
    const centerX = width / 2
    const centerY = height / 2
    
    // 辅助函数：加载图片
    const loadImage = (src) => {
      return new Promise((resolve) => {
        const img = canvas.createImage()
        img.src = src
        img.onload = () => resolve(img)
        img.onerror = (e) => {
          console.error('Canvas图片加载失败:', src, e)
          resolve(null)
        }
      })
    }

    // 2. 绘制背景
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, width, height)
    
    // 应用整体缩放
    const scale = canvasScale.value
    const ropeRadius = r2p(visualRadius.value)
    const beadRadius = r2p(layoutRadius.value)
    
    ctx.save()
    ctx.translate(centerX, centerY)
    ctx.scale(scale, scale)
    ctx.translate(-centerX, -centerY)
    
    // 3. 准备珠子图片 (提前加载)
    const beadList = beads.value
    const beadImages = new Array(beadList.length).fill(null)
    
    // 辅助函数：获取图片路径
    let hasDomainError = false
    const getImagePath = async (url) => {
      if (!url) return null
      return url
    }

    const BATCH_SIZE = 6
    for (let i = 0; i < beadList.length; i += BATCH_SIZE) {
        const batch = beadList.slice(i, i + BATCH_SIZE).map((b, idx) => ({ b, originalIndex: i + idx }))
        
        await Promise.all(batch.map(async ({ b, originalIndex }) => {
            if (!b.imageUrl) return
            
            const path = await getImagePath(b.imageUrl)
            if (path) {
                const img = await loadImage(path)
                if (img) {
                    beadImages[originalIndex] = {
                        img,
                        aspectRatio: img.height / img.width
                    }
                }
            }
        }))
    }
    
    if (hasDomainError) {
        uni.showToast({
            title: '图片加载失败：请在小程序后台配置 downloadFile 域名',
            icon: 'none',
            duration: 3000
        })
    }
    
    // 绘制单个珠子的函数
    const drawBead = (b, i, layout) => {
        const angle = layout.angle
        const beadSizeRpx = Number(b.size || 8) * 7
        const beadSizePx = r2p(beadSizeRpx)
        
        const pendant = isPendant(b)
        const placeRadius = pendant ? (beadRadius + beadSizePx / 2) : beadRadius
        const bx = centerX + placeRadius * Math.cos(angle)
        const by = centerY + placeRadius * Math.sin(angle)
        
        ctx.save()
        ctx.translate(bx, by)
        ctx.rotate(pendant ? (angle - Math.PI / 2) : (angle + Math.PI / 2))
        
        if (b.mirrored) {
          ctx.scale(-1, 1)
        }
        
        const imgInfo = beadImages[i]
        if (imgInfo && imgInfo.img) {
          let drawWidth = beadSizePx
          let drawHeight = beadSizePx
          if (imgInfo.aspectRatio) {
            drawHeight = drawWidth * imgInfo.aspectRatio
          }
          ctx.drawImage(imgInfo.img, -drawWidth / 2, -drawHeight / 2, drawWidth, drawHeight)
        } else {
          ctx.beginPath()
          ctx.arc(0, 0, beadSizePx / 2, 0, 2 * Math.PI)
          ctx.fillStyle = b.color || '#e8e8e8'
          ctx.fill()
        }
        ctx.restore()
    }
    
    // 4. 绘制底层珠子 (大尺寸 >= 24mm)
    for (let i = 0; i < beadList.length; i++) {
      const b = beadList[i]
      const layout = beadLayouts.value[i]
      if (!layout) continue
      
      const sizeMm = Number(b.size || 8)
      if (sizeMm >= 24) {
          drawBead(b, i, layout)
      }
    }

    // 5. 绘制绳子
    ctx.beginPath()
    ctx.strokeStyle = '#e0e0e0'
    ctx.lineWidth = r2p(2)
    ctx.setLineDash([])
    ctx.arc(centerX, centerY, ropeRadius, 0, 2 * Math.PI)
    ctx.stroke()
    
    // 6. 绘制 Logo
    const logoSize = r2p(200)
    try {
      // 尝试加载 Logo
      const logoImg = await loadImage(logoPath)
      if (logoImg) {
        ctx.drawImage(logoImg, centerX - logoSize / 2, centerY - logoSize / 2, logoSize, logoSize)
      } else {
        throw new Error('Logo load failed')
      }
    } catch (e) {
      // 降级绘制
      ctx.beginPath()
      ctx.fillStyle = '#f7f7f7'
      ctx.arc(centerX, centerY, logoSize / 2, 0, 2 * Math.PI)
      ctx.fill()
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillStyle = '#bfbfbf'
      ctx.font = `${r2p(24)}px sans-serif`
      ctx.fillText('念初手作', centerX, centerY)
    }
    
    // 7. 绘制顶层珠子 (普通尺寸 < 24mm)
    for (let i = 0; i < beadList.length; i++) {
      const b = beadList[i]
      const layout = beadLayouts.value[i]
      if (!layout) continue
      
      const sizeMm = Number(b.size || 8)
      if (sizeMm < 24) {
          drawBead(b, i, layout)
      }
    }
    
    ctx.restore() // 恢复全局缩放
    
    // 8. 导出图片
    return new Promise((resolve, reject) => {
      setTimeout(() => { // 稍作延迟确保绘制完成
        uni.canvasToTempFilePath({
          canvas: canvas, // Canvas 2D 必须传实例
          destWidth: width * dpr, // 高清导出
          destHeight: height * dpr,
          fileType: 'jpg',
          quality: 0.9,
          success: (res) => resolve(res.tempFilePath),
          fail: (err) => reject(err)
        })
      }, 100)
    })
    
  } catch (e) {
    uni.hideLoading()
    console.error('Generate Image Error:', e)
    
    let errorDetail = ''
    if (e && e.message) errorDetail += `错误: ${e.message}\n`
    if (e && e.errMsg) errorDetail += `Msg: ${e.errMsg}\n`
    if (JSON.stringify(e) !== '{}') errorDetail += `Detail: ${JSON.stringify(e)}`
    
    uni.showModal({
        title: '生成图片失败',
        content: errorDetail || '未知错误，请重试',
        showCancel: false
    })
    
    throw e
  }
}

// 提交订单
async function submitOrder() {
  if (loading.value) return
  
  try {
    // Generate & Upload Image
    const tempFilePath = await generateDesignImage()
    
    uni.showLoading({ title: '上传设计图...' })
    const uploadRes = await uploadFile(tempFilePath, 'diy_design')
    console.log('Design upload result:', uploadRes)
    
    // 获取返回的图片路径
    let rawPath = ''
    if (typeof uploadRes === 'string') {
        rawPath = uploadRes
    } else if (uploadRes) {
        // 尝试多种可能的字段名
        rawPath = uploadRes.url || uploadRes.path || uploadRes.fileUrl || uploadRes.fileName || ''
        if (!rawPath && uploadRes.data) {
             rawPath = uploadRes.data.url || uploadRes.data.path || ''
        }
    }
    
    console.log('Extracted raw path:', rawPath)

    // 确保是完整的URL
    let designUrl = ''
    if (rawPath) {
        // 使用 resolveImageUrl 统一处理，它会自动判断是否需要添加 /admin/common/image 前缀
        designUrl = resolveImageUrl(rawPath)
    }
    
    console.log('Final design URL:', designUrl)
    
    uni.hideLoading()
    
    // 生成详细的珠子排列描述
    let beadDescription = ''
    if (beads.value.length > 0) {
        beadDescription = '顺时针从顶部到最后一颗描述为：'
        beads.value.forEach((b, index) => {
            const num = index + 1
            const name = b.name || b.title || '未知珠子'
            const size = b.size ? `(${b.size}mm)` : ''
            beadDescription += `第${num}颗：${name}${size}`
            if (index < beads.value.length - 1) {
                beadDescription += '，'
            }
        })
    }

    // Store Data
    const orderData = {
        items: orderItems.value,
        designImage: designUrl,
        categories: activeCategory.value,
        classificationDetail1: null, // 废弃
        classificationDetail2: null, // 废弃
        beadDescription: beadDescription // 新增珠子描述
    }
    
    // 如果有选中的色系，可以作为 classificationDetail1 保存，以便后台参考
    if (activeColor.value) {
        orderData.classificationDetail1 = activeColor.value
    }
    
    uni.setStorageSync('diy_order_data', orderData)
    uni.setStorageSync('diy_order_items', orderItems.value) // Keep compatible
    
    uni.navigateTo({
      url: `/pages/order/confirm?mode=diy&size=${selectedSize.value}`
    })
    showOrderPreview.value = false
    
  } catch (e) {
    uni.hideLoading()
    const msg = e.message || '处理失败'
    uni.showToast({ title: msg.length > 15 ? '处理失败，请查看控制台' : msg, icon: 'none' })
    console.error('Submit Order Error:', e)
  }
}

// 批量加入购物车
async function addDiyToCart() {
  if (!beads.value.length) {
    uni.showToast({ title: '请先添加珠子', icon: 'none' })
    return
  }

  // 统计每个商品的数量
  const map = {}
  beads.value.forEach(b => {
    if (b.productId) {
      map[b.productId] = (map[b.productId] || 0) + 1
    }
  })

  const productIds = Object.keys(map)
  if (!productIds.length) {
    uni.showToast({ title: '没有有效的商品可添加', icon: 'none' })
    return
  }

  uni.showLoading({ title: '正在加入购物车...', mask: true })

  try {
    // 并发调用添加接口
    // 注意：如果商品数量很多，并发过高可能导致问题，这里假设数量有限
    const promises = productIds.map(pid => addToCart(Number(pid), map[pid]))
    
    await Promise.all(promises)
    
    uni.showToast({ title: '已全部加入购物车', icon: 'success' })
    showOrderPreview.value = false
    
    // 更新购物车角标
    updateCartBadgeNow()
    
  } catch (e) {
    console.error('批量添加购物车失败:', e)
    uni.showToast({ title: '部分商品添加失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

onMounted(() => {
  console.log('Design Page Mounted')
  isMounted.value = true
  ensureInit()
})

onShow(() => {
  ensureInit()
})
</script>

<style lang="scss">
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #fff;
}

/* 顶部 Header */
.header {
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
  background: #fff;
  z-index: 100;
  border-bottom: 1rpx solid #f5f5f5;
}

.stat-box {
  background: #fdf8f4;
  padding: 8rpx 20rpx;
  border-radius: 24rpx;
  
  .stat-text {
    font-size: 26rpx;
    color: #d4a574;
    font-weight: 600;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.price-box {
  display: flex;
  align-items: baseline;
  
  .price-label {
    font-size: 24rpx;
    color: #666;
    margin-right: 4rpx;
  }
  .price-num {
    font-size: 32rpx;
    font-weight: bold;
    color: #d4a574;
  }
}

.action-btns {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.cart-btn {
    width: 64rpx;
    height: 64rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fdf8f4;
    border-radius: 50%;
    border: 1rpx solid #f0e0d0;
    
    &:active {
      background: #f0e0d0;
    }
    
    .cart-icon-img {
      width: 36rpx;
      height: 36rpx;
    }
  }

  .checkout-btn {
  margin: 0;
  padding: 0 24rpx;
  height: 56rpx;
  line-height: 56rpx;
  font-size: 24rpx;
  background: #f8f8f8;
  color: #666;
  border-radius: 28rpx;
  border: 1rpx solid #eee;
  
  &::after { border: none; }
  
  &.ready {
    background: #d4a574;
    color: #fff;
    border-color: #d4a574;
  }
}

/* 主区域 */
.main-area {
  flex: 1;
  position: relative;
  background: #fff;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.tip-icon {
  position: absolute;
  top: 20rpx;
  left: 20rpx;
  width: 40rpx;
  height: 40rpx;
  line-height: 40rpx;
  text-align: center;
  border-radius: 50%;
  border: 2rpx solid #d4a574;
  color: #d4a574;
  font-size: 28rpx;
  font-weight: bold;
  z-index: 50;
  background: #fff;
}

.canvas-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding-bottom: 96rpx;
  /* margin-top: -60rpx; 移除上移，避免被Header遮挡 */
}

.canvas {
  width: 560rpx;
  height: 560rpx;
  position: relative;
}

.rope-circle {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 400rpx;
  height: 400rpx;
  border-radius: 50%;
  border: 2rpx solid #e0e0e0;
  z-index: 1;
}

.center-logo {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 0;
  opacity: 0.8;
}

.logo-img {
  width: 180rpx;
  height: 180rpx;
  margin-bottom: 10rpx;
}

.logo-name {
  font-size: 24rpx;
  color: #ccc;
  letter-spacing: 4rpx;
}

/* 底部工具栏 */
.toolbar-container {
  position: absolute;
  left: 10rpx;
  right: 10rpx;
  bottom: 16rpx;
  z-index: 80;
  display: flex;
  align-items: center;
  margin: 0;
  padding: 8rpx;
  background: #f7f7f7;
  border: 1rpx solid rgba(0, 0, 0, 0.05);
  border-radius: 44rpx;
  box-shadow: 0 10rpx 24rpx rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  overflow: visible;
  
  &.collapsed {
    right: auto;
    width: 92rpx;
    .tool-bar {
      flex: 0;
      width: 0;
      opacity: 0;
      padding: 0;
    }
  }
}

.tool-bar {
  flex: 1;
  height: 72rpx;
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 0;
  transition: all 0.3s ease;
  
  .tool-item {
    flex: none;
    height: 56rpx;
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: center;
    padding: 0 18rpx;
    background: #ffffff;
    border: 1rpx solid rgba(0, 0, 0, 0.06);
    border-radius: 32rpx;
    white-space: nowrap;
    
    .tool-icon-img {
      font-size: 30rpx;
      line-height: 30rpx;
      margin-right: 8rpx;
    }
    
    .tool-text {
      margin-top: 0;
      font-size: 24rpx;
      line-height: 24rpx;
      color: rgba(0, 0, 0, 0.78);
    }

    &.active {
      border-color: rgba(0, 0, 0, 0.14);
      background: #ffffff;
      .tool-text {
        color: rgba(0, 0, 0, 0.9);
        font-weight: 600;
      }
    }
  }
}

.tool-collapse-hit {
  width: 72rpx;
  height: 72rpx;
  padding-right: 56rpx;
  margin-right: -56rpx;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  position: relative;
  z-index: 3;
}

.tool-collapse-btn {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  border: 1rpx solid rgba(0, 0, 0, 0.06);
  border-radius: 36rpx;
  position: relative;
  z-index: 2;
  box-shadow: none;
}

.collapse-icon {
  font-size: 44rpx;
  color: rgba(0, 0, 0, 0.45);
}

/* 商品选择区 */
.goods-section {
  height: 600rpx; /* 增加高度，利用工具栏节省的空间 */
  background: #fff;
  display: flex;
  flex-direction: column;
  box-shadow: 0 -4rpx 16rpx rgba(0,0,0,0.05);
}

.section-header {
  height: 80rpx;
  border-bottom: 1rpx solid #f0f0f0;
  display: flex;
  align-items: center;
  padding: 0 20rpx;
}

/* 搜索框样式 */
.search-box {
  position: relative;
  margin-left: 20rpx;
  flex-shrink: 0;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  width: 280rpx;
  height: 56rpx;
  background: #f5f5f5;
  border-radius: 28rpx;
  padding: 0 20rpx;
}

.search-icon {
  font-size: 24rpx;
  margin-right: 12rpx;
  color: #999;
}

.search-input {
  flex: 1;
  height: 100%;
  font-size: 24rpx;
  color: #333;
  background: transparent;
  border: none;
  outline: none;
}

.search-input::placeholder {
  color: #bbb;
}

.search-clear {
  font-size: 20rpx;
  color: #999;
  padding: 8rpx;
  margin-left: 8rpx;
}

/* 搜索结果下拉框 */
.search-dropdown {
  position: absolute;
  top: 64rpx;
  right: 0;
  width: 400rpx;
  max-height: 600rpx;
  background: #fff;
  border-radius: 16rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.15);
  z-index: 1000;
  overflow: hidden;
}

.search-dropdown-scroll {
  max-height: 600rpx;
}

.search-result-item {
  display: flex;
  align-items: center;
  padding: 20rpx;
  border-bottom: 1rpx solid #f5f5f5;
  transition: background 0.2s;
}

.search-result-item:active {
  background: #f9f9f9;
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-img {
  width: 80rpx;
  height: 80rpx;
  border-radius: 12rpx;
  background: #f5f5f5;
  flex-shrink: 0;
}

.search-result-color {
  width: 80rpx;
  height: 80rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
}

.search-result-info {
  flex: 1;
  margin-left: 20rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}

.search-result-name {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.search-result-category {
  font-size: 22rpx;
  color: #999;
  margin-top: 6rpx;
}

.search-result-price {
  font-size: 28rpx;
  color: #ff6b6b;
  font-weight: 600;
  margin-left: 16rpx;
}

.search-empty {
  padding: 40rpx;
  text-align: center;
  font-size: 28rpx;
  color: #999;
}

.category-tabs {
  white-space: nowrap;
  height: 100%;
  line-height: 80rpx;
  flex: 1;
  overflow: hidden;
}

.cat-tab {
  display: inline-block;
  padding: 0 30rpx;
  font-size: 28rpx;
  color: #999;
  position: relative;
  
  &.active {
    color: #333;
    font-weight: bold;
    font-size: 30rpx;
    
    &::after {
      content: '';
      position: absolute;
      bottom: 16rpx;
      left: 50%;
      transform: translateX(-50%);
      width: 40rpx;
      height: 6rpx;
      background: #d4a574;
      border-radius: 4rpx;
    }
  }
}

.section-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 色系侧栏 */
.color-sidebar {
  width: 160rpx;
  background: #f8f8f8;
  
  .sidebar-group {
    margin-bottom: 10rpx;
  }
  
  .group-divider {
    height: 2rpx;
    background: #e0e0e0;
    margin: 0;
  }

  .color-tag {
    height: 90rpx;
    line-height: 90rpx;
    text-align: center;
    font-size: 26rpx;
    color: #666;
    
    &.active {
      background: #fff;
      color: #d4a574;
      font-weight: bold;
      position: relative;
      
      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 30rpx;
        bottom: 30rpx;
        width: 6rpx;
        background: #d4a574;
        border-radius: 0 4rpx 4rpx 0;
      }
    }
  }
}

/* 商品列表 Grid */
.product-area {
  flex: 1;
  background: #fff;
  padding: 20rpx;
}

.product-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20rpx;
  padding-bottom: 40rpx;
}

.product-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.08);
  padding-bottom: 16rpx;
  
  .p-visual {
    width: 100%;
    position: relative;
    padding-bottom: 100%; /* 正方形 */
    margin-bottom: 8rpx;
    
    .p-bead {
      position: absolute;
      top: 0; left: 0; right: 0; bottom: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      
      .p-img {
        width: 70%;
        height: 70%;
        opacity: 0;
        transition: opacity 0.5s ease;
      }
      
      .p-img.loaded {
        opacity: 1;
      }
    }
    
    .p-stock {
      position: absolute;
      top: 12rpx;
      left: 12rpx;
      background: rgba(0,0,0,0.6);
      color: #fff;
      font-size: 20rpx;
      padding: 4rpx 12rpx;
      border-radius: 20rpx;
      z-index: 2;
    }
  }
  
  .p-name {
    font-size: 24rpx;
    color: #333;
    font-weight: bold;
    width: 96%;
    text-align: center;
    
    /* 改为双行显示 */
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    overflow: hidden;
    white-space: normal;
    
    margin-bottom: 8rpx;
    height: 72rpx; /* 固定高度，确保对齐 */
    line-height: 36rpx;
  }
  
  .p-price {
    font-size: 28rpx;
    color: #333;
    font-weight: bold;
    margin-bottom: 4rpx;
  }
  
  .p-size {
    font-size: 22rpx;
    color: #999;
  }
}

/* 珠子样式 */
.bead {
  position: absolute;
  /* border-radius: 50%; 移除强制圆角，允许异形配饰 */
  z-index: 10;
}
.bead-img {
  width: 100%;
  height: 100%;
  /* border-radius: 50%; 移除强制圆角 */
}
.bead-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}
.swap-indicator {
  position: absolute;
  top: 0;
  right: 0;
  background: #d4a574;
  color: #fff;
  font-size: 20rpx;
  width: 30rpx;
  height: 30rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 飞行动画 */
.flying-bead {
  position: fixed;
  top: 50%;
  left: 50%;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  z-index: 999;
  pointer-events: none;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.15);
}

@keyframes flyToCenter {
  0% { transform: translate(-50%, 400rpx) scale(0.5); opacity: 0; }
  20% { opacity: 1; }
  100% { transform: translate(-50%, -50%); opacity: 0; }
}

.export-canvas {
  position: absolute;
  left: -9999rpx;
  width: 560rpx;
  height: 560rpx;
  background: #fff;
}

/* 弹窗通用 */
.mask {
  position: fixed;
  left: 0; right: 0; top: 0; bottom: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.popup {
  width: 85%;
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
}
.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 28rpx;
  border-bottom: 1rpx solid #f0f0f0;
}
.popup-title { font-size: 30rpx; font-weight: 700; color: #333; }
.popup-close { font-size: 32rpx; color: #999; padding: 8rpx; }
.popup-body { padding: 24rpx 28rpx; }
.popup-btn {
  margin: 20rpx 28rpx 28rpx;
  height: 80rpx;
  line-height: 80rpx;
  background: linear-gradient(135deg, #d4a574, #c9976c);
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 40rpx;
  border: none;
}

/* 戴法选择 */
.wear-style-section {
  margin-bottom: 32rpx;
}
.section-label {
  display: block;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 20rpx;
  font-weight: 500;
}
.wear-style-options {
  display: flex;
  gap: 20rpx;
}
.wear-style-opt {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 24rpx;
  background: #f8f8f8;
  border-radius: 16rpx;
  border: 2rpx solid transparent;
  transition: all 0.2s ease;
}
.wear-style-opt.selected {
  background: #fdf8f3;
  border-color: #d4a574;
}
.style-radio {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  border: 2rpx solid #ccc;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  flex-shrink: 0;
}
.wear-style-opt.selected .style-radio {
  border-color: #d4a574;
}
.style-radio-inner {
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  background: #d4a574;
}
.style-info {
  display: flex;
  flex-direction: column;
}
.style-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 6rpx;
}
.style-desc {
  font-size: 22rpx;
  color: #999;
}

/* 尺寸选择 */
.size-section {
  margin-bottom: 20rpx;
}

/* 尺寸选项 */
.size-options {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12rpx;
}
.size-options.compact { grid-template-columns: repeat(4, 1fr); }
.size-opt {
  padding: 16rpx 8rpx;
  background: #f8f8f8;
  border-radius: 12rpx;
  text-align: center;
  border: 2rpx solid transparent;
  transition: all 0.2s ease;
}
.size-opt.selected { background: #fdf8f3; border-color: #d4a574; }
.opt-num { font-size: 28rpx; font-weight: 600; color: #333; }
.opt-unit { font-size: 18rpx; color: #999; margin-left: 2rpx; }

.custom-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx;
  background: #f8f8f8;
  border-radius: 12rpx;
  margin-top: 10rpx;
}
.custom-label { font-size: 24rpx; color: #666; }
.custom-ipt { flex: 1; height: 48rpx; font-size: 28rpx; background: transparent; }
.custom-unit { font-size: 24rpx; color: #999; }

/* 预览弹窗 */
.preview-popup { max-height: 80vh; }
.preview-body { padding: 20rpx 28rpx; max-height: 50vh; overflow-y: auto; }
.preview-meta {
  display: flex;
  justify-content: center;
  gap: 24rpx;
  font-size: 24rpx;
  color: #888;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid #f0f0f0;
  margin-bottom: 16rpx;
}
.preview-items { max-height: 240rpx; overflow-y: auto; }
.pv-item {
  display: flex;
  align-items: center;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f8f8f8;
}
.pv-name { flex: 1; font-size: 26rpx; color: #333; }
.pv-qty { font-size: 24rpx; color: #999; margin-right: 16rpx; }
.pv-price { font-size: 26rpx; color: #d4a574; font-weight: 600; }
.preview-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-top: 2rpx solid #f0f0f0;
  margin-top: 12rpx;
  font-size: 28rpx;
  color: #333;
}
.pv-total-num { font-size: 36rpx; color: #d4a574; font-weight: 700; }
.preview-actions {
  display: flex;
  gap: 16rpx;
  padding: 0 28rpx 28rpx;
}
.act-cancel, .act-submit {
  flex: 1;
  height: 76rpx;
  line-height: 76rpx;
  border-radius: 38rpx;
  font-size: 28rpx;
  font-weight: 600;
  border: none;
}
.act-cancel { background: #f5f5f5; color: #666; }
.act-submit { background: linear-gradient(135deg, #d4a574, #c9976c); color: #fff; }

/* 引导弹窗 */
.guide-popup { text-align: center; padding: 40rpx 28rpx; }
.guide-icon { font-size: 64rpx; margin-bottom: 16rpx; }
.guide-title { display: block; font-size: 32rpx; font-weight: 700; color: #333; margin-bottom: 12rpx; }
.guide-desc { display: block; font-size: 24rpx; color: #999; margin-bottom: 28rpx; }
/* 操作指南弹窗 */
.help-popup {
  width: 640rpx;
  background: #fff;
  border-radius: 32rpx;
  overflow: hidden;
}

.header-deco {
  font-size: 32rpx;
  margin-right: 12rpx;
}

.help-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24rpx;
  padding: 10rpx 30rpx 30rpx;
}

.help-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #f9f9f9;
  padding: 24rpx 12rpx;
  border-radius: 20rpx;
}

.help-icon-box {
  position: relative;
  width: 120rpx;
  height: 120rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16rpx;
}

.help-text {
  font-size: 24rpx;
  color: #333;
  text-align: center;
}

/* 演示动画图标 */
.demo-bead {
  width: 40rpx;
  height: 40rpx;
  background: #e6c5a1;
  border-radius: 50%;
  animation: demo-pop 1.5s infinite;
}

.demo-hand {
  position: absolute;
  bottom: 20rpx;
  right: 20rpx;
  font-size: 36rpx;
  animation: demo-tap 1.5s infinite;
}

.demo-circle {
  width: 32rpx;
  height: 32rpx;
  background: #e6c5a1;
  border-radius: 50%;
  position: absolute;
}

.demo-drag-hand {
  position: absolute;
  font-size: 36rpx;
  animation: demo-drag 2s infinite linear;
}

.demo-bead-mirror {
  width: 40rpx;
  height: 40rpx;
  background: linear-gradient(90deg, #e6c5a1 50%, #d4a574 50%);
  border-radius: 50%;
  animation: demo-flip 2s infinite;
}

.demo-toolbar {
  display: flex;
  gap: 6rpx;
}

.dt-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #ddd;
}

.dt-dot.active {
  background: #d4a574;
  animation: demo-blink 1.5s infinite;
}

/* 动画定义 */
@keyframes demo-pop {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}

@keyframes demo-tap {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-10rpx, -10rpx); }
}

@keyframes demo-drag {
  0% { top: 60rpx; left: 60rpx; }
  25% { top: 20rpx; left: 60rpx; }
  50% { top: 20rpx; left: 20rpx; }
  75% { top: 60rpx; left: 20rpx; }
  100% { top: 60rpx; left: 60rpx; }
}

@keyframes demo-flip {
  0%, 100% { transform: scaleX(1); }
  50% { transform: scaleX(-1); }
}

@keyframes demo-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

@keyframes demo-pinch-1 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(12rpx, 12rpx); }
}

@keyframes demo-pinch-2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-12rpx, -12rpx); }
}

.demo-pinch {
  position: relative;
  width: 60rpx;
  height: 60rpx;
}

.finger {
  position: absolute;
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  background-color: #d4a574;
  opacity: 0.6;
}

.finger-1 {
  top: 0;
  left: 0;
  animation: demo-pinch-1 2s infinite ease-in-out;
}

.finger-2 {
  bottom: 0;
  right: 0;
  animation: demo-pinch-2 2s infinite ease-in-out;
}

.help-footer {
  padding: 0 30rpx 30rpx;
}

.checkbox-row {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20rpx;
  opacity: 0.8;
}

.checkbox {
  width: 32rpx;
  height: 32rpx;
  border: 2rpx solid #ccc;
  border-radius: 50%;
  margin-right: 12rpx;
  position: relative;
}

.checkbox.checked {
  background: #d4a574;
  border-color: #d4a574;
}

.checkbox.checked::after {
  content: '';
  position: absolute;
  left: 10rpx;
  top: 6rpx;
  width: 8rpx;
  height: 14rpx;
  border-right: 4rpx solid #fff;
  border-bottom: 4rpx solid #fff;
  transform: rotate(45deg);
}

.checkbox-label {
  font-size: 24rpx;
  color: #999;
}

.help-btn {
  margin: 0;
  height: 80rpx;
  line-height: 80rpx;
  background: linear-gradient(135deg, #d4a574, #c9976c);
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 40rpx;
}

/* Loading States */
.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40rpx 0;
  color: #999;
  font-size: 26rpx;
}

.loading-dot {
  width: 40rpx;
  height: 40rpx;
  border: 4rpx solid #f3f3f3;
  border-top: 4rpx solid #d4a574;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16rpx;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-more {
  text-align: center;
  padding: 20rpx 0;
  color: #999;
  font-size: 24rpx;
}
</style>
