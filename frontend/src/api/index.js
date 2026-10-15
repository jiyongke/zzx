import request from './request'

// ============ 认证 ============
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const getInfo = () => request.get('/auth/info')

// ============ 分类 ============
export const getCategories = () => request.get('/category/list')
export const adminCategories = () => request.get('/category/admin/list')
export const saveCategory = (data) => request.post('/category', data)
export const updateCategory = (id, data) => request.put(`/category/${id}`, data)
export const deleteCategory = (id) => request.delete(`/category/${id}`)

// ============ 商品 ============
export const getProducts = (params) => request.get('/product/list', { params })
export const getHotProducts = (limit = 8) => request.get('/product/hot', { params: { limit } })
export const getProductDetail = (id) => request.get(`/product/detail/${id}`)
export const adminProducts = (params) => request.get('/product/admin/list', { params })
export const saveProduct = (data) => request.post('/product', data)
export const updateProduct = (id, data) => request.put(`/product/${id}`, data)
export const updateProductStatus = (id, status) => request.put(`/product/${id}/status`, null, { params: { status } })
export const deleteProduct = (id) => request.delete(`/product/${id}`)
export const farmerProducts = (params) => request.get('/product/farmer/list', { params })
export const farmerSaveProduct = (data) => request.post('/product/farmer', data)
export const farmerUpdateProduct = (id, data) => request.put(`/product/farmer/${id}`, data)
export const farmerApplyProduct = (id) => request.put(`/product/farmer/${id}/apply`)

// ============ 购物车 ============
export const getCart = () => request.get('/cart/list')
export const addCart = (data) => request.post('/cart/add', data)
export const updateCart = (id, quantity) => request.put(`/cart/${id}`, { quantity })
export const deleteCart = (id) => request.delete(`/cart/${id}`)
export const clearCart = () => request.delete('/cart/clear')

// ============ 订单 ============
export const createOrder = (data) => request.post('/order/create', data)
export const getMyOrders = (params) => request.get('/order/list', { params })
export const getOrderDetail = (id) => request.get(`/order/${id}`)
export const payOrder = (id) => request.put(`/order/${id}/pay`)
export const cancelOrder = (id) => request.put(`/order/${id}/cancel`)
export const confirmOrder = (id) => request.put(`/order/${id}/confirm`)
export const shipOrder = (id, data) => request.put(`/order/${id}/ship`, null, { params: data })
export const updateDelivery = (id, deliveryStatus) => request.put(`/order/${id}/delivery`, null, { params: { deliveryStatus } })
export const refundApply = (id, reason) => request.post(`/order/${id}/refund-apply`, null, { params: { reason } })
export const refundHandle = (id, approve) => request.put(`/order/admin/${id}/refund-handle`, null, { params: { approve } })
export const adminOrders = (params) => request.get('/order/admin/list', { params })

// ============ 溯源 ============
export const getTraceBatches = () => request.get('/trace/list')
export const getTraceBatch = (id) => request.get(`/trace/batch/${id}`)
export const queryTrace = (code) => request.get('/trace/query', { params: { code } })
export const adminTraceBatches = (params) => request.get('/trace/admin/list', { params })
export const adminTraceBatch = (id) => request.get(`/trace/admin/batch/${id}`)
export const createBatch = (data) => request.post('/trace/batch', data)
export const updateBatch = (id, data) => request.put(`/trace/batch/${id}`, data)
export const publishBatch = (id) => request.put(`/trace/batch/${id}/publish`)
export const reviewTraceBatch = (id, approve, remark) => request.put(`/trace/batch/${id}/review`, null, { params: { approve, remark } })
export const offlineBatch = (id) => request.put(`/trace/batch/${id}/offline`)
export const deleteBatch = (id) => request.delete(`/trace/batch/${id}`)
export const addTraceNode = (data) => request.post('/trace/node', data)
export const deleteTraceNode = (id) => request.delete(`/trace/node/${id}`)

// ============ 用户/地址/管理 ============
export const updateProfile = (params) => request.put('/user/profile', null, { params })
export const changePassword = (data) => request.put('/user/password', data)
export const getAddresses = () => request.get('/user/address/list')
export const addAddress = (data) => request.post('/user/address', data)
export const updateAddress = (id, data) => request.put(`/user/address/${id}`, data)
export const deleteAddress = (id) => request.delete(`/user/address/${id}`)

// ============ 农户资质认证（准入可信） ============
export const getQualification = () => request.get('/user/qualification')
export const submitQualification = (data) => request.post('/user/qualification', data)
export const adminQualifications = (params) => request.get('/admin/qualification/list', { params })
export const reviewQualification = (id, approve, remark) => request.put(`/admin/qualification/${id}/review`, null, { params: { approve, remark } })
export const adminUsers = (params) => request.get('/admin/user/list', { params })
export const updateUserStatus = (id, status) => request.put(`/admin/user/${id}/status`, null, { params: { status } })
export const updateUserRole = (id, role) => request.put(`/admin/user/${id}/role`, null, { params: { role } })
export const adminCreateUser = (data, role) => request.post('/admin/user', data, { params: { role } })
export const getDashboard = () => request.get('/admin/dashboard')
export const getDailyReport = (date) => request.get('/admin/report/daily', { params: { date } })
export const getWeeklyReport = (endDate) => request.get('/admin/report/weekly', { params: { endDate } })
export const getAuditLogs = (params) => request.get('/admin/audit/list', { params })
