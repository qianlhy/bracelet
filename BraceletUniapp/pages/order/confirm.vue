<template>
  <view class="page">
    <!-- 收货地址 -->
    <view class="section">
      <view class="section-title">收货地址</view>
      <view v-if="!selectedAddress" class="no-address" @click="goSelectAddress">
        <view class="icon-box"><text class="icon">📍</text></view>
        <text class="tip">请选择收货地址</text>
        <text class="arrow">›</text>
      </view>
      <view v-else class="address-card" @click="goSelectAddress">
        <view class="icon-box"><text class="icon">📍</text></view>
        <view class="addr-info">
          <view class="addr-header">
            <text class="name">{{ selectedAddress.name }}</text>
            <text class="phone">{{ selectedAddress.phone }}</text>
          </view>
          <view class="addr-detail">
            {{ selectedAddress.province }} {{ selectedAddress.city }} {{ selectedAddress.district }} {{ selectedAddress.detail }}
          </view>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- 商品列表 -->
    <view class="section">
      <view class="section-title">商品清单</view>
      
      <!-- DIY 模式显示单一汇总项 -->
      <view v-if="orderMode === 'diy'" class="goods-list">
        <view class="goods-item">
          <image class="goods-thumb" :src="designImage" mode="aspectFill" @click="previewDesign" />
          <view class="goods-info">
            <view class="goods-title">DIY商品：{{ diyName || '未命名' }}</view>
            <view class="goods-meta">
              <text class="price">¥{{ totalAmount }}</text>
              <text class="qty">x1</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 普通模式显示商品列表 -->
      <view v-else class="goods-list">
        <view v-for="item in cartItems" :key="item.id" class="goods-item">
          <image v-if="item.imageUrl || item.image" class="goods-thumb" :src="item.imageUrl || item.image" mode="aspectFill" />
          <view v-else-if="item.color" class="goods-thumb" :style="{background: item.color}"></view>
          <view v-else class="goods-thumb"></view>
          <view class="goods-info">
            <view class="goods-title">{{ item.title }}</view>
            <view class="goods-meta">
              <text class="price">¥{{ item.price }}</text>
              <text class="qty">x{{ item.quantity }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- DIY信息 (仅DIY模式显示) -->
    <view class="section" v-if="orderMode === 'diy'">
      <view class="section-title">DIY信息</view>
      
      <view class="diy-info-row">
        <text class="label">作品名称</text>
        <input class="diy-input" type="text" v-model="diyName" placeholder="请输入作品名称" maxlength="20" />
      </view>
      
      <view class="diy-info-row design-row">
        <text class="label">设计图</text>
        <image :src="designImage" mode="aspectFit" class="small-preview-img" @click="previewDesign" />
      </view>
    </view>

    <!-- 订单备注 -->
    <view class="section">
      <view class="section-title">订单备注</view>
      <input class="remark-input" type="text" v-model="remark" placeholder="选填，请先和商家协商一致" />
    </view>

    <!-- 订单金额 -->
    <view class="section">
      <view class="amount-row">
        <text class="label">商品金额</text>
        <text class="value">¥{{ totalAmount }}</text>
      </view>
      <view class="amount-row">
        <text class="label">运费</text>
        <text class="value">¥{{ shippingFee.toFixed(2) }}</text>
      </view>
      <view class="amount-row total">
        <text class="label">实付款</text>
        <text class="value">¥{{ finalAmount }}</text>
      </view>
    </view>

    <!-- 底部提交栏 -->
    <view class="submit-bar">
      <view class="total-info">
        <text class="label">合计：</text>
        <text class="amount">¥{{ finalAmount }}</text>
      </view>
      <view class="btn-group">
        <button class="action-btn submit-btn" :disabled="!selectedAddress || submitting" @click="submitOrder">
          {{ submitting ? '提交中...' : '提交订单' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { onLoad } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { addressList, cartAdd, cartList, clearCart, createDiyOrder, orderCreate } from '../../api/index.js'
import { resolveImageUrl } from '../../utils/imageHelper.js'

const cartItems = ref([])
const selectedAddress = ref(null)
const submitting = ref(false)
const orderMode = ref('cart') // cart or diy
const diyItems = ref([])
const designImage = ref('')
const remark = ref('')
const diyName = ref('')

const totalAmount = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0).toFixed(2)
})

// 运费计算：新疆、西藏加15元，其他省份0元
const shippingFee = computed(() => {
  if (!selectedAddress.value || !selectedAddress.value.province) {
    return 0
  }
  const province = selectedAddress.value.province
  if (province.includes('新疆') || province.includes('西藏')) {
    return 15
  }
  return 0
})

// 实付款 = 商品金额 + 运费
const finalAmount = computed(() => {
  return (Number(totalAmount.value) + shippingFee.value).toFixed(2)
})

const classificationDetails = ref({})
const classificationDetailKeys = ref({})
const orderCategories = ref('')
const beadDescription = ref('')

onLoad(async (options) => {
  // 检查是否是DIY订单
  if (options.mode === 'diy') {
    orderMode.value = 'diy'
    try {
      // 优先尝试获取完整数据
      const data = uni.getStorageSync('diy_order_data')
      if (data && data.items) {
        diyItems.value = data.items
        cartItems.value = data.items
        designImage.value = data.designImage
        beadDescription.value = data.beadDescription || '' // 获取珠子描述
        // 获取分类详情 (1-8)
        for (let i = 1; i <= 8; i++) {
            if (data[`classificationDetail${i}`]) {
                classificationDetails.value[i] = data[`classificationDetail${i}`]
            }
            if (data[`classificationDetailKey${i}`]) {
                classificationDetailKeys.value[i] = data[`classificationDetailKey${i}`]
            }
        }
        orderCategories.value = data.categories || ''
      } else {
        // 兼容旧格式
        const items = uni.getStorageSync('diy_order_items')
        if (items && Array.isArray(items)) {
          diyItems.value = items
          cartItems.value = items
        }
      }
      
      // 如果传递了尺寸参数，自动填充到备注
      if (options.size) {
        remark.value = `手链尺寸: ${options.size}cm`
      }
      
    } catch (e) {
      console.error('获取DIY订单数据失败', e)
    }
  } else if (options.mode === 'direct') {
    orderMode.value = 'direct'
    try {
      const item = uni.getStorageSync('direct_buy_item')
      if (item) {
        // 确保图片路径处理
        item.imageUrl = resolveImageUrl(item.imageUrl)
        item.image = resolveImageUrl(item.image)
        cartItems.value = [item]
      }
    } catch (e) {
      console.error('获取购买商品失败', e)
    }
  } else {
    await loadCartItems()
  }
  await loadDefaultAddress()
})

function previewDesign() {
  if (designImage.value) {
    uni.previewImage({ urls: [designImage.value] })
  }
}



// 加载购物车商品
async function loadCartItems() {
  try {
    const res = await cartList()
    const items = res.items || []
    
    // 处理图片路径
    cartItems.value = items.map(item => {
      let imageUrl = item.imageUrl || item.image || ''
      return {
        ...item,
        imageUrl: resolveImageUrl(imageUrl),
        image: resolveImageUrl(imageUrl)
      }
    })
    
    if (cartItems.value.length === 0) {
      uni.showToast({ title: '购物车为空', icon: 'none' })
      setTimeout(() => {
        uni.navigateBack()
      }, 1500)
    }
  } catch (e) {
    console.error('加载购物车失败:', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

// 加载默认地址
async function loadDefaultAddress() {
  try {
    const res = await addressList()
    const addresses = Array.isArray(res) ? res : (res.data || [])
    // 查找默认地址
    selectedAddress.value = addresses.find(addr => addr.isDefault) || addresses[0] || null
  } catch (e) {
    console.error('加载地址失败:', e)
  }
}

// 选择地址
function goSelectAddress() {
  uni.navigateTo({ 
    url: '/pages/address/select',
    events: {
      // 监听地址选择事件
      selectAddress: (address) => {
        selectedAddress.value = address
      }
    }
  })
}

// 提交订单
async function submitOrder() {
  if (!selectedAddress.value) {
    uni.showToast({ title: '请选择收货地址', icon: 'none' })
    return
  }

  if (submitting.value) return
  
  submitting.value = true
  uni.showLoading({ title: '提交中...' })

  try {
    let res
    
    if (orderMode.value === 'diy') {
      // 构造符合API文档的数据结构
      // items: [{ price, productId, quantity, title }]
      // 动态使用 diyName 作为 items 中的 title (如果存在)
      const currentDiyName = diyName.value || '我的设计'
      const items = diyItems.value.map(item => ({
        productId: item.productId || item.id || 0,
        title: currentDiyName, // 强制将 title 绑定为 diyName
        price: item.price,
        quantity: item.quantity
      }))

      // 构建描述信息
      // 优先使用 beadDescription (珠子排列描述)
      // 如果没有，则使用分类描述作为后备
      let desc = beadDescription.value
      
      if (!desc) {
          desc = `分类:${orderCategories.value || ''}`
          // classificationDetails[1] 存储的是色系名称/子分类名称
          if (classificationDetails.value[1]) {
            const detail = classificationDetails.value[1]
            // 兼容处理：如果是字符串直接拼，如果是对象取name
            const name = (typeof detail === 'object' && detail.name) ? detail.name : detail
            desc += ` ${name}`
          }
      }

      res = await createDiyOrder({
        // 基础字段
        receiverName: selectedAddress.value.name,
        receiverPhone: selectedAddress.value.phone,
        receiverProvince: selectedAddress.value.province,
        receiverCity: selectedAddress.value.city,
        receiverDistrict: selectedAddress.value.district,
        receiverDetail: selectedAddress.value.detail,
        remark: remark.value || 'DIY设计订单',
        
        // DIY特定字段
        items: items,
        diyImage: designImage.value,
        diyName: diyName.value || '我的设计',
        description: desc.trim(),
        designId: 0
      })
    } else if (orderMode.value === 'direct') {
      // 直接购买：先清空购物车，确保只购买当前商品
      try {
        await clearCart()
      } catch (e) {
        console.warn('清空购物车失败，继续执行', e)
      }

      // 再加入购物车，再创建订单 (因为 /user/order/submit 404)
      for (const item of cartItems.value) {
          const pid = item.productId || item.id
          if (pid) {
              await cartAdd(Number(pid), Number(item.quantity || 1))
          }
      }

      // 使用购物车创建订单接口
      res = await orderCreate({
        addressId: selectedAddress.value.id,
        receiverName: selectedAddress.value.name,
        receiverPhone: selectedAddress.value.phone,
        receiverProvince: selectedAddress.value.province,
        receiverCity: selectedAddress.value.city,
        receiverDistrict: selectedAddress.value.district,
        receiverDetail: selectedAddress.value.detail,
        remark: remark.value || '直接购买订单'
      })
    } else {
      res = await orderCreate({
        addressId: selectedAddress.value.id,
        receiverName: selectedAddress.value.name,
        receiverPhone: selectedAddress.value.phone,
        receiverProvince: selectedAddress.value.province,
        receiverCity: selectedAddress.value.city,
        receiverDistrict: selectedAddress.value.district,
        receiverDetail: selectedAddress.value.detail
      })
    }
    
    uni.hideLoading()
    
    // 获取订单ID (优先使用 id, 其次 orderId)
    const orderId = res.order?.id || res.id || res.order?.orderId || res.orderId
    
    if (!orderId) {
      console.error('订单创建成功但无法获取订单ID:', res)
      uni.showToast({ title: '订单创建失败', icon: 'none' })
      return
    }
    
    // 记录订单过期时间 (30分钟后)
    try {
      const expireTime = Date.now() + 30 * 60 * 1000
      uni.setStorageSync('order_expire_' + orderId, expireTime)
    } catch (e) {
      console.error('保存订单过期时间失败', e)
    }

    uni.showToast({ title: '订单创建成功', icon: 'success' })
    
    // 跳转到订单详情页
    setTimeout(() => {
      uni.redirectTo({ url: '/pages/order/detail?id=' + orderId })
    }, 1500)
    
  } catch (e) {
    uni.hideLoading()
    console.error('创建订单失败:', e)
    uni.showToast({ title: e.message || '创建订单失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f7f7;
  padding-bottom: 140rpx;
}

.section {
  background: #fff;
  margin-bottom: 20rpx;
  padding: 24rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #333;
  margin-bottom: 24rpx;
}

/* 收货地址 */
.no-address, .address-card {
  display: flex;
  align-items: center;
  padding: 0;
  background: #fff;
}

.icon-box {
  width: 60rpx;
  height: 60rpx;
  background: #ffb84d;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
}

.no-address .icon, .address-card .icon {
  font-size: 32rpx;
  color: #fff;
}

.no-address .tip {
  flex: 1;
  font-size: 30rpx;
  color: #333;
  font-weight: 600;
}

.addr-info {
  flex: 1;
}

.addr-header {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 8rpx;
}

.addr-header .name {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.addr-header .phone {
  font-size: 28rpx;
  color: #666;
}

.addr-detail {
  font-size: 26rpx;
  color: #999;
}

.arrow {
  font-size: 32rpx;
  color: #ccc;
  margin-left: 20rpx;
}

/* DIY 信息 */
.diy-info-row {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}

.diy-info-row:last-child {
  margin-bottom: 0;
}

.diy-info-row .label {
  width: 140rpx;
  font-size: 28rpx;
  color: #666;
}

.diy-input {
  flex: 1;
  height: 80rpx;
  background: #f9f9f9;
  border-radius: 8rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  color: #333;
}

.small-preview-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: 8rpx;
  background: #f9f9f9;
}

/* 商品列表 */
.goods-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.goods-item {
  display: flex;
  gap: 20rpx;
}

.goods-thumb {
  width: 120rpx;
  height: 120rpx;
  background: #f0f0f0;
  border-radius: 12rpx;
  flex-shrink: 0;
}

.goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 4rpx 0;
}

.goods-title {
  font-size: 28rpx;
  color: #333;
  line-height: 1.4;
}

.goods-meta {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.goods-meta .price {
  font-size: 32rpx;
  font-weight: 700;
  color: #ff6b6b;
}

.goods-meta .qty {
  font-size: 24rpx;
  color: #999;
}

/* 备注输入框 */
.remark-input {
  width: 100%;
  height: 80rpx;
  background: #f9f9f9;
  border-radius: 8rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
}

/* 金额 */
.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 0;
  font-size: 28rpx;
}

.amount-row .label {
  color: #666;
}

.amount-row .value {
  color: #333;
}

.amount-row.total {
  border-top: 2rpx solid #f0f0f0;
  padding-top: 20rpx;
  margin-top: 10rpx;
}

.amount-row.total .label {
  font-size: 30rpx;
  font-weight: 700;
}

.amount-row.total .value {
  font-size: 36rpx;
  font-weight: 700;
  color: #ff6b6b;
}

/* 提交栏 */
.submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  background: #fff;
  border-top: 2rpx solid #f0f0f0;
  box-shadow: 0 -4rpx 12rpx rgba(0, 0, 0, 0.05);
  z-index: 100;
}

.total-info {
  display: flex;
  align-items: baseline;
}

.total-info .label {
  font-size: 28rpx;
  color: #666;
}

.total-info .amount {
  font-size: 36rpx;
  font-weight: 700;
  color: #ff6b6b;
}

.btn-group {
  display: flex;
  gap: 20rpx;
}

.action-btn {
  padding: 0 40rpx;
  height: 72rpx;
  line-height: 72rpx;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 36rpx;
  border: none;
  margin: 0;
}

.cart-btn {
  background: #333;
  color: #fff;
}

.submit-btn {
  background: linear-gradient(135deg, #ffd84c, #ffb84d);
  color: #333;
}

.submit-btn[disabled] {
  background: #ddd;
  color: #999;
}
</style>
