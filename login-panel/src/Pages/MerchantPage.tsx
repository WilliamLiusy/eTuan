import React, { useState, useEffect } from 'react';
import {
    Box, AppBar, Toolbar, Typography, Button, Grid, Card, CardContent,
    CardActions, TextField, Dialog, DialogTitle, DialogContent, DialogActions,
    List, ListItem, ListItemText, Divider, Alert, CircularProgress,
    Tabs, Tab, IconButton, Chip
} from '@mui/material';
import {
    Add, Delete, Store, Logout, ShoppingBag, Assignment
} from '@mui/icons-material';
import { MerchantAddProductMessage } from '../Plugins/ProductService/APIs/MerchantAddProductMessage';
import { MerchantRemoveProductMessage } from '../Plugins/ProductService/APIs/MerchantRemoveProductMessage';
import { FetchProductsByMerchantIDMessage } from '../Plugins/ProductService/APIs/FetchProductsByMerchantIDMessage';
import { GetOrderDetails } from '../Plugins/OrderService/APIs/GetOrderDetails';
import { UpdateOrderStatus } from '../Plugins/OrderService/APIs/UpdateOrderStatus';
import { GetUserInfoByToken } from '../Plugins/UserCenter/APIs/GetUserInfoByToken';
import { getUserToken, setUserToken } from '../Globals/GlobalStore';
import { ProductInfo } from '../Plugins/ProductService/Objects/ProductInfo';
import { OrderInfo } from '../Plugins/OrderService/Objects/OrderInfo';
import { OrderStatus } from '../Plugins/OrderService/Objects/OrderStatus';

interface TabPanelProps {
    children?: React.ReactNode;
    index: number;
    value: number;
}

function TabPanel(props: TabPanelProps) {
    const { children, value, index, ...other } = props;
    return (
        <div
            role="tabpanel"
            hidden={value !== index}
            id={`simple-tabpanel-${index}`}
            aria-labelledby={`simple-tab-${index}`}
            {...other}
        >
            {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
        </div>
    );
}

// 通用响应处理函数
const parseApiResponse = (response: string, defaultMessage?: string): string => {
    try {
        const parsed = JSON.parse(response);
        if (typeof parsed === 'string') {
            return parsed;
        }
        return defaultMessage || '操作成功';
    } catch {
        return response || defaultMessage || '操作成功';
    }
};

const MerchantPage: React.FC = () => {
    const [tabValue, setTabValue] = useState(0);
    const [products, setProducts] = useState<ProductInfo[]>([]);
    const [orders, setOrders] = useState<OrderInfo[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [userInfo, setUserInfo] = useState<any>(null);

    // 添加商品对话框状态
    const [addProductOpen, setAddProductOpen] = useState(false);
    const [newProductName, setNewProductName] = useState('');
    const [newProductPrice, setNewProductPrice] = useState('');
    const [newProductDescription, setNewProductDescription] = useState('');

    // 订单详情弹窗状态
    const [detailOpen, setDetailOpen] = useState(false);
    const [detailOrder, setDetailOrder] = useState<OrderInfo | null>(null);

    useEffect(() => {
        loadUserInfo();
    }, []);

    // 当用户信息加载完成后，加载商品和订单
    useEffect(() => {
        if (userInfo?.userID) {
            loadProducts();
            loadOrders();
        }
    }, [userInfo]);

    const loadUserInfo = async () => {
        const token = getUserToken();
        if (token) {
            const getUserInfoMsg = new GetUserInfoByToken(token);
            getUserInfoMsg.send(
                (userInfoStr: string) => {
                    try {
                        const info = JSON.parse(userInfoStr);
                        setUserInfo(info);
                    } catch (err) {
                        console.error('解析用户信息失败:', err);
                    }
                },
                (error: any) => {
                    console.error('获取用户信息失败:', error);
                }
            );
        }
    };

    const loadProducts = async () => {
        const token = getUserToken();
        if (token && userInfo?.userID) {
            setLoading(true);
            console.log('加载商家商品，商家ID:', userInfo.userID); // 添加调试日志
            const getProductsMsg = new FetchProductsByMerchantIDMessage(userInfo.userID);
            getProductsMsg.send(
                (productsStr: string) => {
                    try {
                        const productList = JSON.parse(productsStr);
                        setProducts(productList);
                    } catch (err) {
                        setError('解析商品数据失败');
                    } finally {
                        setLoading(false);
                    }
                },
                (error: any) => {
                    setError('获取商品列表失败');
                    setLoading(false);
                }
            );
        }
    };

    const loadOrders = async () => {
        // 商家订单查询：用 QueryOrdersByUser(token) 查询所有与该商家相关的订单
        const token = getUserToken();
        if (!token || !userInfo?.userID) {
            setOrders([]);
            return;
        }
        setLoading(true);
        try {
            const queryOrdersMsg = new (require('../Plugins/OrderService/APIs/QueryOrdersByUser').QueryOrdersByUser)(token);
            queryOrdersMsg.send(
                (ordersStr: string) => {
                    try {
                        let orderList = [];
                        try {
                            orderList = JSON.parse(ordersStr);
                        } catch {
                            orderList = [];
                        }
                        // 只保留 merchantID 匹配当前商家的订单
                        const merchantOrders = Array.isArray(orderList)
                            ? orderList.filter((order: any) => order.merchantID === userInfo.userID)
                            : [];
                        setOrders(merchantOrders);
                    } catch (err) {
                        setError('解析订单数据失败');
                        setOrders([]);
                    } finally {
                        setLoading(false);
                    }
                },
                (error: any) => {
                    setError('获取订单失败：' + (error?.message || '未知错误'));
                    setOrders([]);
                    setLoading(false);
                }
            );
        } catch (err) {
            setError('获取订单失败，请稍后重试');
            setOrders([]);
            setLoading(false);
        }
    };

    const handleAddProduct = async () => {
        if (!newProductName.trim() || !newProductPrice.trim() || !newProductDescription.trim()) {
            setError('请填写完整的商品信息');
            return;
        }

        const price = parseFloat(newProductPrice);
        if (isNaN(price) || price <= 0) {
            setError('请输入有效的商品价格');
            return;
        }

        setLoading(true);
        const token = getUserToken();

        try {
            const addProductMsg = new MerchantAddProductMessage(
                token || '',
                newProductName.trim(),
                price,
                newProductDescription.trim()
            );

            addProductMsg.send(
                (result: string) => {
                    const message = parseApiResponse(result, '商品添加成功');
                    setSuccess(message);
                    setAddProductOpen(false);
                    setNewProductName('');
                    setNewProductPrice('');
                    setNewProductDescription('');
                    loadProducts(); // 重新加载商品列表
                    setLoading(false);
                },
                (error: any) => {
                    setError('添加商品失败：' + (error?.message || '未知错误'));
                    setLoading(false);
                }
            );
        } catch (err) {
            setError('添加商品失败，请稍后重试');
            setLoading(false);
        }
    };

    const handleRemoveProduct = async (productName: string) => {
        if (!confirm(`确定要删除商品 "${productName}" 吗？`)) {
            return;
        }

        setLoading(true);
        const token = getUserToken();

        try {
            const removeProductMsg = new MerchantRemoveProductMessage(
                token || '',
                productName
            );

            removeProductMsg.send(
                (result: string) => {
                    const message = parseApiResponse(result, '商品删除成功');
                    setSuccess(message);
                    loadProducts(); // 重新加载商品列表
                    setLoading(false);
                },
                (error: any) => {
                    setError('删除商品失败：' + (error?.message || '未知错误'));
                    setLoading(false);
                }
            );
        } catch (err) {
            setError('删除商品失败，请稍后重试');
            setLoading(false);
        }
    };

    // 获取下一个订单状态
    const getNextStatus = (currentStatus: OrderStatus): OrderStatus | null => {
        switch (currentStatus) {
            case OrderStatus.waitingForDish:
                return OrderStatus.delivering;
            case OrderStatus.delivering:
                return OrderStatus.completed;
            default:
                return null;
        }
    };
    // 状态按钮文案
    const getNextStatusText = (currentStatus: OrderStatus): string => {
        switch (currentStatus) {
            case OrderStatus.waitingForDish:
                return '开始配送';
            case OrderStatus.delivering:
                return '订单完成';
            default:
                return '';
        }
    };
    // 修改订单状态
    const handleUpdateOrderStatus = async (orderID: string, newStatus: OrderStatus) => {
        setLoading(true);
        try {
            const updateStatusMsg = new UpdateOrderStatus(orderID, newStatus);
            updateStatusMsg.send(
                (result: string) => {
                    setSuccess('订单状态已更新');
                    loadOrders();
                    setLoading(false);
                },
                (error: any) => {
                    setError('更新订单状态失败：' + (error?.message || '未知错误'));
                    setLoading(false);
                }
            );
        } catch (err) {
            setError('更新订单状态失败，请稍后重试');
            setLoading(false);
        }
    };

    const handleLogout = () => {
        setUserToken('');
        window.location.hash = '#/login';
    };

    return (
        <Box sx={{ flexGrow: 1 }}>
            {/* 顶部导航栏 */}
            <AppBar position="static">
                <Toolbar>
                    <Store sx={{ mr: 2 }} />
                    <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
                        外卖系统 - 商家管理
                    </Typography>
                    <Typography variant="body2" sx={{ mr: 2 }}>
                        欢迎，{userInfo?.name || '商家'}
                    </Typography>
                    <Button color="inherit" onClick={handleLogout} startIcon={<Logout />}>
                        登出
                    </Button>
                </Toolbar>
            </AppBar>

            {/* 错误和成功提示 */}
            {error && (
                <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
                    {error}
                </Alert>
            )}
            {success && (
                <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess('')}>
                    {success}
                </Alert>
            )}

            {/* 标签页 */}
            <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
                <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)}>
                    <Tab label="商品管理" icon={<ShoppingBag />} />
                    <Tab label="订单管理" icon={<Assignment />} />
                </Tabs>
            </Box>

            {/* 商品管理 */}
            <TabPanel value={tabValue} index={0}>
                <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
                    <Typography variant="h5">
                        我的商品
                    </Typography>
                    <Button
                        variant="contained"
                        startIcon={<Add />}
                        onClick={() => setAddProductOpen(true)}
                    >
                        添加商品
                    </Button>
                </Box>

                {loading ? (
                    <Box display="flex" justifyContent="center" p={4}>
                        <CircularProgress />
                    </Box>
                ) : (
                    <Grid container spacing={3}>
                        {products.length === 0 ? (
                            <Grid item xs={12}>
                                <Typography color="text.secondary" align="center">
                                    暂无商品，点击"添加商品"开始添加
                                </Typography>
                            </Grid>
                        ) : (
                            products.map((product) => (
                                <Grid item xs={12} sm={6} md={4} key={product.productID}>
                                    <Card>
                                        <CardContent>
                                            <Typography variant="h6">{product.name}</Typography>
                                            <Typography color="text.secondary" paragraph>
                                                {product.description}
                                            </Typography>
                                            <Typography variant="h6" color="primary">
                                                ¥{product.price.toFixed(2)}
                                            </Typography>
                                        </CardContent>
                                        <CardActions>
                                            <Button
                                                size="small"
                                                color="error"
                                                startIcon={<Delete />}
                                                onClick={() => handleRemoveProduct(product.name)}
                                            >
                                                删除
                                            </Button>
                                        </CardActions>
                                    </Card>
                                </Grid>
                            ))
                        )}
                    </Grid>
                )}
            </TabPanel>

            {/* 订单管理 */}
            <TabPanel value={tabValue} index={1}>
                <Typography variant="h5" gutterBottom>
                    订单管理
                </Typography>

                {orders.length === 0 ? (
                    <Typography color="text.secondary">
                        暂无订单
                    </Typography>
                ) : (
                    <Grid container spacing={3}>
                        {orders.map((order) => (
                            <Grid item xs={12} md={6} key={order.orderID}>
                                <Card>
                                    <CardContent>
                                        <Typography variant="h6">
                                            订单 #{order.orderID.substring(0, 8)}...
                                        </Typography>
                                        <Typography color="text.secondary">
                                            顾客: {order.customerID}
                                        </Typography>
                                        <Typography>
                                            配送地址: {order.destinationAddress}
                                        </Typography>
                                        <Typography>
                                            商品数量: {order.productList?.length || 0}
                                        </Typography>
                                        <Chip
                                            label={order.orderStatus}
                                            color={order.orderStatus === '已完成' ? 'success' : 'primary'}
                                            sx={{ mt: 1 }}
                                        />
                                    </CardContent>
                                    <CardActions>
                                        <Button size="small" onClick={() => { setDetailOrder(order); setDetailOpen(true); }}>
                                            查看详情
                                        </Button>
                                        {getNextStatus(order.orderStatus) && (
                                            <Button
                                                size="small"
                                                variant="contained"
                                                color="primary"
                                                onClick={() => handleUpdateOrderStatus(order.orderID, getNextStatus(order.orderStatus)!)}
                                                disabled={loading}
                                            >
                                                {getNextStatusText(order.orderStatus)}
                                            </Button>
                                        )}
                                    </CardActions>
                                </Card>
                            </Grid>
                        ))}
                    </Grid>
                )}
            </TabPanel>

            {/* 添加商品对话框 */}
            <Dialog open={addProductOpen} onClose={() => setAddProductOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>添加新商品</DialogTitle>
                <DialogContent>
                    <TextField
                        autoFocus
                        margin="dense"
                        label="商品名称"
                        fullWidth
                        variant="outlined"
                        value={newProductName}
                        onChange={(e) => setNewProductName(e.target.value)}
                        sx={{ mb: 2 }}
                    />
                    <TextField
                        margin="dense"
                        label="商品价格"
                        type="number"
                        fullWidth
                        variant="outlined"
                        value={newProductPrice}
                        onChange={(e) => setNewProductPrice(e.target.value)}
                        inputProps={{ min: 0, step: 0.01 }}
                        sx={{ mb: 2 }}
                    />
                    <TextField
                        margin="dense"
                        label="商品描述"
                        fullWidth
                        multiline
                        rows={3}
                        variant="outlined"
                        value={newProductDescription}
                        onChange={(e) => setNewProductDescription(e.target.value)}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setAddProductOpen(false)}>取消</Button>
                    <Button
                        onClick={handleAddProduct}
                        variant="contained"
                        disabled={loading}
                    >
                        {loading ? <CircularProgress size={20} /> : '添加'}
                    </Button>
                </DialogActions>
            </Dialog>

            {/* 订单详情弹窗 */}
            <Dialog open={detailOpen} onClose={() => setDetailOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>订单详情</DialogTitle>
                <DialogContent>
                    {detailOrder ? (
                        <>
                            <Typography>订单号：{detailOrder.orderID}</Typography>
                            <Typography>顾客：{detailOrder.customerID}</Typography>
                            <Typography>配送地址：{detailOrder.destinationAddress}</Typography>
                            <Typography>商品数量：{detailOrder.productList?.length || 0}</Typography>
                            <Typography>状态：{detailOrder.orderStatus}</Typography>
                            <Box mt={2}>
                                <Typography variant="subtitle1">商品详情：</Typography>
                                {Array.isArray(detailOrder.productList) && detailOrder.productList.length > 0 ? (
                                    <List dense>
                                        {detailOrder.productList.map((item: any, idx: number) => (
                                            <ListItem key={idx} divider>
                                                <ListItemText
                                                    primary={item.name ? `${item.name} × ${item.count || 1}` : JSON.stringify(item)}
                                                    secondary={item.price ? `单价：¥${item.price}` : ''}
                                                />
                                            </ListItem>
                                        ))}
                                    </List>
                                ) : (
                                    <Typography color="text.secondary">无商品信息</Typography>
                                )}
                            </Box>
                        </>
                    ) : (
                        <Typography>无订单信息</Typography>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setDetailOpen(false)}>关闭</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default MerchantPage;
