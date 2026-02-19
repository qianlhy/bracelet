<template>
  <view class="page">
    <view v-if="!orders.length && !isLoading" class="empty">暂无订单</view>
    <view v-else class="list">
      <view class="card" v-for="o in orders" :key="o.id || o.orderId" @click="goDetail(o.id || o.orderId)">
        <view class="head">
          <view class="no">订单号 {{ o.orderNo || o.order_no || o.id || o.orderId }}</view>
          <view class="head-right">
            <view v-if="o.status === 0 && o.countdownStr" class="countdown-tag">
              <text>剩余 {{ o.countdownStr }}</text>
            </view>
            <view class="chip" :class="'s'+(o.status||0)">{{ o.statusText || statusText(o.status) }}</view>
          </view>
        </view>
        <view class="body">
          <view class="thumb">
            <image v-if="getOrderImage(o)" :src="getOrderImage(o)" mode="aspectFill" style="width:100%;height:100%;border-radius:12rpx;" />
          </view>
          <view class="meta">
            <view class="amount">¥{{ o.amount }}</view>
            <!-- 接口未返回数量时，默认不显示或显示1 -->
            <view class="sub" v-if="o.totalNum || o.productCount">共 {{ o.totalNum || o.productCount }} 件</view>
          </view>
        </view>
        <view class="footer">
          <view class="tracking-row" v-if="o.status === 2 && (o.trackingNumber || o.tracking_number)" @click.stop>
            <text class="label">快递单号：</text>
            <text class="value">{{ o.trackingNumber || o.tracking_number }}</text>
            <view class="copy-tag" @click.stop="copyText(o.trackingNumber || o.tracking_number)">复制</view>
          </view>
          <view class="actions">
            <view class="tip" v-if="o.status === 4" style="color: #ff9800;">如等待时间过长请加客服了解</view>
            <view class="tip" v-else>联系客服具体了解商品详情</view>
            <!-- 状态 0:待支付, 1:已支付(待发货), 2:已发货, 3:已完成, 4:退款审核中, 5:退款中, 6:已退款 -->
            <view class="btn-cancel" v-if="o.status === 0" @click.stop="handleCancel(o)">取消订单</view>
            <view class="btn-pay" v-if="o.status === 0" @click.stop="handlePay(o)">去支付</view>
            <!-- 仅未发货(已支付状态)显示退款按钮 -->
            <view class="btn-refund" v-if="(o.status === 1)" @click.stop="handleRefund(o)">申请退款</view>
          </view>
        </view>
      </view>
    </view>
    <!-- <view class="loading-more" v-if="orders.length > 0">
      {{ hasMore ? '加载中...' : '没有更多了' }}
    </view> -->
  </view>
  
</template>

<script setup>
import { ref } from 'vue'
import { orderList, refundOrder, cancelOrder, payOrder } from '../../api/index.js'
import { resolveImageUrl } from '../../utils/imageHelper.js'
import { onShow, onLoad, onPullDownRefresh, onReachBottom, onUnload, onHide } from '@dcloudio/uni-app'

const orders = ref([])
const page = ref(1)
const pageSize = ref(10)
const hasMore = ref(true)
const isLoading = ref(false)
let filterStatus = null
let timer = null

function startTimer() {
  stopTimer()
  updateCountdowns() // 立即执行一次
  timer = setInterval(() => {
    updateCountdowns()
  }, 1000)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function updateCountdowns() {
  const now = Date.now()
  orders.value.forEach(o => {
    if (o.status === 0) { // 仅处理待支付
      let expireTime = 0
      const id = o.id || o.orderId
      
      // 1. 优先尝试从本地缓存获取 (confirm.vue 存入的)
      if (id) {
        const cached = uni.getStorageSync('order_expire_' + id)
        if (cached) expireTime = Number(cached)
      }
      
      // 2. 如果缓存没有，尝试使用 createTime + 30m
      if (!expireTime && (o.createTime || o.create_time)) {
        let ct = o.createTime || o.create_time
        // iOS兼容处理
        if (typeof ct === 'string') ct = ct.replace(/-/g, '/')
        expireTime = new Date(ct).getTime() + 30 * 60 * 1000
      }
      
      if (expireTime > now) {
        const diff = Math.floor((expireTime - now) / 1000)
        const m = Math.floor(diff / 60)
        const s = diff % 60
        o.countdownStr = `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
      } else if (expireTime > 0) {
        o.countdownStr = '00:00'
      } else {
        o.countdownStr = ''
      }
    } else {
      o.countdownStr = ''
    }
  })
}

function getOrderImage(order) {
  if (!order) return ''
  
  // 优先检查DIY图片字段 (兼容多种命名)
  let diyImg = order.productImage || order.product_image || order.diyImage || order.designImage || order.image || order.img || order.pic
  
  // 如果没有直接的图片字段，尝试从 items[0].productImage 获取（针对某些后端结构）
  if (!diyImg && order.items && order.items.length > 0) {
      diyImg = order.items[0].productImage || order.items[0].image || order.items[0].imageUrl || order.items[0].pic
  }

  if (diyImg) {
      return resolveImageUrl(String(diyImg).trim())
  }
  
  return ''
}

function goDetail(id) {
  console.log('点击订单，ID:', id, '类型:', typeof id)
  // 只检查是否存在，不检查字符串值
  if (!id && id !== 0) {
    console.error('订单ID无效:', id)
    uni.showToast({ title: '订单ID无效', icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages/order/detail?id=' + id })
}

function copyText(text) {
  if (!text) return
  uni.setClipboardData({
    data: String(text),
    success: () => {
      uni.showToast({ title: '复制成功', icon: 'none' })
    }
  })
}

async function handleRefund(order) {
  const id = order.id || order.orderId
  uni.showModal({
    title: '申请退款',
    content: '确定要申请退款吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          uni.showLoading({ title: '提交中...' })
          await refundOrder(id)
          uni.hideLoading()
          uni.showToast({ title: '申请退款成功', icon: 'success' })
          // 刷新列表
          setTimeout(() => {
            refresh()
          }, 1500)
        } catch (e) {
          uni.hideLoading()
          console.error('退款申请失败:', e)
          uni.showToast({ title: e.msg || '申请失败，请重试', icon: 'none' })
        }
      }
    }
  })
}

async function handleCancel(order) {
  const id = order.id || order.orderId
  uni.showModal({
    title: '取消订单',
    content: '确定要取消此订单吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          uni.showLoading({ title: '处理中...' })
          await cancelOrder(id)
          uni.hideLoading()
          uni.showToast({ title: '订单已取消', icon: 'success' })
          setTimeout(() => refresh(), 1500)
        } catch (e) {
          uni.hideLoading()
          console.error('取消失败:', e)
          uni.showToast({ title: e.msg || '取消失败', icon: 'none' })
        }
      }
    }
  })
}

async function handlePay(order) {
  const orderNo = order.orderNo || order.order_no
  if (!orderNo) return uni.showToast({ title: '订单号缺失', icon: 'none' })
  
  try {
    uni.showLoading({ title: '正在支付...' })
    // 默认使用微信支付(1)
    await payOrder(orderNo, 1)
    uni.hideLoading()
    uni.showToast({ title: '支付成功', icon: 'success' })
    setTimeout(() => refresh(), 1500)
  } catch (e) {
    uni.hideLoading()
    console.error('支付失败:', e)
    uni.showToast({ title: e.msg || '支付失败', icon: 'none' })
  }
}

function statusText(s) {
  const m = { 0: '待支付', 1: '已支付', 2: '已发货', 3: '已完成', 4: '退款审核中', 5: '退款中', 6: '已退款' }
  return m[s] ?? s
}

function extractList(res) {
  if (res && res.orders) return res.orders
  if (res && res.records) return res.records
  if (Array.isArray(res)) return res
  if (res && res.data) {
    if (res.data.orders) return res.data.orders
    return Array.isArray(res.data) ? res.data : (res.data.records || [])
  }
  return []
}

async function load(isRefresh = false) {
  if (isLoading.value) return
  isLoading.value = true
  
  if (isRefresh) {
    page.value = 1
    hasMore.value = true
  }

  try {
    const params = { 
      page: page.value, 
      size: pageSize.value 
    }
    
    let list = []
    
    // 如果是已支付(1)，同时获取退款审核中(4)
    // 采用双重请求并行获取，合并结果，解决后端不支持 OR 查询的问题
    if (filterStatus !== null && filterStatus === 1) {
      const p1 = orderList({ ...params, status: 1 })
      const p2 = orderList({ ...params, status: 4 })
      
      const [res1, res2] = await Promise.all([p1, p2])
      const list1 = extractList(res1)
      const list2 = extractList(res2)
      
      // 合并并按ID倒序（近似时间倒序）
      list = [...list1, ...list2].sort((a, b) => {
        const id1 = a.id || a.orderId || 0
        const id2 = b.id || b.orderId || 0
        return id2 - id1
      })
      
      // 只有当两个列表都取不满时，才认为没有更多了
      if (list1.length < pageSize.value && list2.length < pageSize.value) {
        hasMore.value = false
      } else {
        page.value++
      }

    } else {
      // 常规单状态查询
      if (filterStatus !== null) {
        params.status = filterStatus
      }
      
      const res = await orderList(params)
      list = extractList(res)
      
      if (list.length < pageSize.value) {
        hasMore.value = false
      } else {
        page.value++
      }
    }
    
    console.log(`📋 加载完成，数量:`, list.length)

    if (isRefresh) {
      orders.value = list
    } else {
      orders.value = [...orders.value, ...list]
    }
    
    // 启动倒计时
    startTimer()
    
  } catch (e) {
    console.error('❌ 加载订单列表失败:', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    isLoading.value = false
    uni.stopPullDownRefresh()
  }
}

function refresh() {
  load(true)
}

onShow(() => {
  // 每次显示时刷新第一页，保证状态最新
  // 延迟一点避免页面切换动画卡顿
  setTimeout(() => {
    refresh()
  }, 300)
})

onLoad((options) => {
  if (options && options.status !== undefined) {
    const s = Number(options.status)
    filterStatus = Number.isNaN(s) ? null : s
  }
})

onPullDownRefresh(() => {
  refresh()
})

onReachBottom(() => {
  if (hasMore.value) {
    load()
  }
})

onHide(() => {
  stopTimer()
})

onUnload(() => {
  stopTimer()
})
</script>

<style>
.page { padding: 24rpx; background: #f7f7f7; min-height: 100vh; box-sizing: border-box; }
.empty { padding: 120rpx 24rpx; text-align: center; color: #999; }
.list { display: flex; flex-direction: column; gap: 16rpx; }
.card { background: #ffffff; border-radius: 16rpx; padding: 16rpx; box-shadow: 0 6rpx 16rpx rgba(0,0,0,0.04); }
.head { display: flex; justify-content: space-between; align-items: center; }
.no { color: #666; font-size: 24rpx; }
.chip { padding: 4rpx 14rpx; border-radius: 999rpx; font-size: 22rpx; }
.chip.s0 { background: #fff3e0; color: #a66a00; }
.chip.s1 { background: #e8f5e9; color: #2e7d32; }
.chip.s2 { background: #e3f2fd; color: #1565c0; }
.chip.s3 { background: #ede7f6; color: #5e35b1; }
.body { display: flex; gap: 16rpx; margin-top: 12rpx; }
.thumb { width: 140rpx; height: 140rpx; background: #e9eef3; border-radius: 12rpx; }
.meta { display: flex; flex-direction: column; justify-content: center; }
.amount { color: #e54d42; font-size: 32rpx; font-weight: 700; }
.sub { color: #888; margin-top: 6rpx; }
.footer { margin-top: 12rpx; padding-top: 12rpx; border-top: 1rpx solid #f0f0f0; }
.actions { display: flex; justify-content: space-between; align-items: center; }
.tip { text-align: right; color: #999; font-size: 22rpx; }
.tracking-row {
  display: flex;
  align-items: center;
  font-size: 24rpx;
  color: #666;
  margin-bottom: 12rpx;
  padding-bottom: 12rpx;
  border-bottom: 1rpx dashed #f0f0f0;
}
.tracking-row .value {
  color: #333;
  margin-left: 8rpx;
  font-family: monospace;
  flex: 1;
}
.tracking-row .copy-tag {
  color: #1565c0;
  margin-left: 16rpx;
  padding: 4rpx 12rpx;
  background: #e3f2fd;
  border-radius: 8rpx;
  font-size: 20rpx;
}
.btn-cancel {
  font-size: 24rpx;
  color: #999;
  border: 1rpx solid #ddd;
  padding: 8rpx 20rpx;
  border-radius: 30rpx;
  background: #fff;
  margin-right: 16rpx;
}
.btn-refund { 
  font-size: 24rpx; 
  color: #666; 
  border: 1rpx solid #ddd; 
  padding: 8rpx 20rpx; 
  border-radius: 30rpx;
  background: #fff;
}
.btn-pay {
  font-size: 24rpx;
  color: #333;
  border: 1rpx solid #ffd84c;
  padding: 8rpx 28rpx;
  border-radius: 30rpx;
  background: #ffd84c;
  font-weight: 600;
  margin-left: 16rpx;
}
.head-right {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.countdown-tag {
  color: #ff4d4f;
  font-size: 22rpx;
  font-weight: bold;
  background: rgba(255, 77, 79, 0.1);
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
}
</style>
