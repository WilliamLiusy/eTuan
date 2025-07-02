import React, { useState, useEffect } from 'react';
import {
    Box, AppBar, Toolbar, Typography, Button, Grid, Card, CardContent,
    CardActions, TextField, List, ListItem, ListItemText, Badge,
    IconButton, Dialog, DialogTitle, DialogContent, DialogActions,
    Chip, Divider, Alert, CircularProgress, Tabs, Tab
} from '@mui/material';
import {
    ShoppingCart, Add, Remove, Store, LocalDining, Logout
} from '@mui/icons-material';
import { GetAllMerchants } from '../Plugins/UserCenter/APIs/GetAllMerchants';
import { FetchProductsByMerchantIDMessage } from '../Plugins/ProductService/APIs/FetchProductsByMerchantIDMessage';
import { CreateOrder } from '../Plugins/OrderService/APIs/CreateOrder';
import { QueryOrdersByUser } from '../Plugins/OrderService/APIs/QueryOrdersByUser';
import { GetUserInfoByToken } from '../Plugins/UserCenter/APIs/GetUserInfoByToken';
import { getUserToken, setUserToken } from '../Globals/GlobalStore';
import { OrderInfo } from '../Plugins/OrderService/Objects/OrderInfo';
import { ProductInfo } from '../Plugins/ProductService/Objects/ProductInfo';
import { OrderStatus } from '../Plugins/OrderService/Objects/OrderStatus';

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

interface CartItem {
    product: ProductInfo;
    quantity: number;
}

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

const OrderPage: React.FC = () => {
    const [tabValue, setTabValue] = useState(0);
    const [merchants, setMerchants] = useState<any[]>([]);
    const [selectedMerchant, setSelectedMerchant] = useState<any>(null);
    const [products, setProducts] = useState<ProductInfo[]>([]);
    const [cart, setCart] = useState<CartItem[]>([]);
    const [orders, setOrders] = useState<OrderInfo[]>([]);
    const [cartOpen, setCartOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [userInfo, setUserInfo] = useState<any>(null);
    const [deliveryAddress, setDeliveryAddress] = useState(''); // 配送地址

    // 组件加载时获取用户信息和商家列表
    useEffect(() => {
        loadUserInfo();
        loadMerchants();
        loadOrders();
    }, []);

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

    const loadMerchants = async () => {
        setLoading(true);
        const getMerchantsMsg = new GetAllMerchants();
        getMerchantsMsg.send(
            (merchantsStr: string) => {
                try {
                    const merchantList = JSON.parse(merchantsStr);
                    setMerchants(merchantList);
                } catch (err) {
                    setError('解析商家数据失败');
                } finally {
                    setLoading(false);
                }
            },
            (error: any) => {
                setError('获取商家列表失败');
                setLoading(false);
            }
        );
    };

    const loadProducts = async (merchantId: string) => {
        setLoading(true);
        const getProductsMsg = new FetchProductsByMerchantIDMessage(merchantId);
        console.log('请求商品列表，商家ID:', merchantId);
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
                console.error('获取商品列表失败:', error);
                setError('获取商品列表失败：' + (error?.message));
            }
        );
    };

    const loadOrders = async () => {
        const token = getUserToken();
        if (token) {
            const queryOrdersMsg = new QueryOrdersByUser(token);
            queryOrdersMsg.send(
                (ordersStr: string) => {
                    try {
                        const parsedOrders = JSON.parse(ordersStr);
                        setOrders(parsedOrders);
                    } catch (err) {
                        console.error('解析订单数据失败:', err);
                        setError('解析订单数据失败');
                    }
                },
                (error: any) => {
                    console.error('获取订单列表失败:', error);
                    setError('获取订单列表失败：' + (error?.message));
                }
            );
        }
    };

    const handleMerchantSelect = (merchant: any) => {
        setSelectedMerchant(merchant);
        loadProducts(merchant.userID);
        setTabValue(1); // 切换到商品标签页
    };

    const addToCart = (product: ProductInfo) => {
        setCart(prevCart => {
            const existingItem = prevCart.find(item => item.product.productID === product.productID);
            if (existingItem) {
                return prevCart.map(item =>
                    item.product.productID === product.productID
                        ? { ...item, quantity: item.quantity + 1 }
                        : item
                );
            } else {
                return [...prevCart, { product, quantity: 1 }];
            }
        });
    };

    const removeFromCart = (productId: string) => {
        setCart(prevCart => {
            return prevCart.map(item =>
                item.product.productID === productId
                    ? { ...item, quantity: item.quantity - 1 }
                    : item
            ).filter(item => item.quantity > 0);
        });
    };

    const getTotalPrice = () => {
        return cart.reduce((total, item) => total + (item.product.price * item.quantity), 0);
    };

    const getTotalItems = () => {
        return cart.reduce((total, item) => total + item.quantity, 0);
    };

    const handleCheckout = async () => {
        if (cart.length === 0) {
            setError('购物车为空');
            return;
        }

        if (!selectedMerchant) {
            setError('请选择商家');
            return;
        }

        if (!deliveryAddress.trim()) {
            setError('请填写配送地址');
            return;
        }

        setLoading(true);
        const token = getUserToken();

        try {
            // 构建商品列表
            const productList: ProductInfo[] = cart.map(item =>
                new ProductInfo(
                    item.product.productID,
                    item.product.merchantID,
                    item.product.name,
                    item.product.price * item.quantity, // 总价
                    `数量: ${item.quantity}`
                )
            );

            const createOrderMsg = new CreateOrder(
                token || '',
                selectedMerchant.userID,
                productList,
                deliveryAddress.trim() // 使用用户输入的配送地址
            );

            createOrderMsg.send(
                (result: string) => {
                    const message = parseApiResponse(result, '下单成功！');
                    console.log('下单成功:', message);

                    // 下单成功
                    setCart([]); // 清空购物车
                    setDeliveryAddress(''); // 清空地址
                    setCartOpen(false);
                    loadOrders(); // 重新加载订单列表
                    setTabValue(2); // 切换到订单标签页
                    setLoading(false);
                },
                (error: any) => {
                    setError('下单失败：' + (error?.message || '未知错误'));
                    setLoading(false);
                }
            );
        } catch (err) {
            setError('下单失败，请稍后重试');
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
                    <LocalDining sx={{ mr: 2 }} />
                    <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
                        外卖订餐系统 - 顾客端
                    </Typography>
                    <Typography variant="body2" sx={{ mr: 2 }}>
                        欢迎，{userInfo?.name || '用户'}
                    </Typography>
                    <IconButton
                        color="inherit"
                        onClick={() => setCartOpen(true)}
                        sx={{ mr: 2 }}
                    >
                        <Badge badgeContent={getTotalItems()} color="error">
                            <ShoppingCart />
                        </Badge>
                    </IconButton>
                    <Button color="inherit" onClick={handleLogout} startIcon={<Logout />}>
                        登出
                    </Button>
                </Toolbar>
            </AppBar>

            {/* 错误提示 */}
            {error && (
                <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
                    {error}
                </Alert>
            )}

            {/* 标签页 */}
            <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
                <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)}>
                    <Tab label="商家列表" />
                    <Tab label="商品浏览" disabled={!selectedMerchant} />
                    <Tab label="我的订单" />
                </Tabs>
            </Box>

            {/* 商家列表 */}
            <TabPanel value={tabValue} index={0}>
                <Typography variant="h5" gutterBottom>
                    选择商家
                </Typography>
                {loading ? (
                    <Box display="flex" justifyContent="center" p={4}>
                        <CircularProgress />
                    </Box>
                ) : (
                    <Grid container spacing={3}>
                        {merchants.map((merchant) => (
                            <Grid item xs={12} sm={6} md={4} key={merchant.userID}>
                                <Card>
                                    <CardContent>
                                        <Typography variant="h6">{merchant.name}</Typography>
                                        <Typography color="text.secondary">
                                            {merchant.address || '暂无地址'}
                                        </Typography>
                                        <Typography variant="body2" color="text.secondary">
                                            联系方式: {merchant.contactNumber}
                                        </Typography>
                                    </CardContent>
                                    <CardActions>
                                        <Button size="small" onClick={() => handleMerchantSelect(merchant)}>
                                            选择此商家
                                        </Button>
                                    </CardActions>
                                </Card>
                            </Grid>
                        ))}
                    </Grid>
                )}
            </TabPanel>

            {/* 商品浏览 */}
            <TabPanel value={tabValue} index={1}>
                {selectedMerchant && (
                    <>
                        <Typography variant="h5" gutterBottom>
                            {selectedMerchant.name} - 商品列表
                        </Typography>
                        {loading ? (
                            <Box display="flex" justifyContent="center" p={4}>
                                <CircularProgress />
                            </Box>
                        ) : (
                            <Grid container spacing={3}>
                                {products.map((product) => (
                                    <Grid item xs={12} sm={6} md={4} key={product.productID}>
                                        <Card>
                                            <CardContent>
                                                <Typography variant="h6">{product.name}</Typography>
                                                <Typography color="text.secondary">
                                                    {product.description || '暂无描述'}
                                                </Typography>
                                                <Typography variant="h6" color="primary" sx={{ mt: 2 }}>
                                                    ¥{product.price.toFixed(2)}
                                                </Typography>
                                            </CardContent>
                                            <CardActions>
                                                <Button
                                                    size="small"
                                                    variant="contained"
                                                    onClick={() => addToCart(product)}
                                                    startIcon={<Add />}
                                                >
                                                    加入购物车
                                                </Button>
                                            </CardActions>
                                        </Card>
                                    </Grid>
                                ))}
                            </Grid>
                        )}
                    </>
                )}
            </TabPanel>

            {/* 我的订单 */}
            <TabPanel value={tabValue} index={2}>
                <Typography variant="h5" gutterBottom>
                    我的订单
                </Typography>
                {orders.length === 0 ? (
                    <Typography>暂无订单</Typography>
                ) : (
                    <Grid container spacing={3}>
                        {orders.map((order) => (
                            <Grid item xs={12} md={6} key={order.orderID}>
                                <Card>
                                    <CardContent>
                                        <Typography variant="h6">
                                            订单 #{order.orderID?.substring(0, 8) || 'N/A'}...
                                        </Typography>
                                        <Typography color="text.secondary">
                                            商家ID: {order.merchantID || 'N/A'}
                                        </Typography>
                                        <Typography color="text.secondary">
                                            配送地址: {order.destinationAddress || 'N/A'}
                                        </Typography>
                                        <Typography>
                                            商品数量: {Array.isArray(order.productList) ? order.productList.length : 0}
                                        </Typography>
                                        {/* 显示商品详情 */}
                                        {Array.isArray(order.productList) && order.productList.length > 0 && (
                                            <Box sx={{ mt: 1 }}>
                                                <Typography variant="body2" color="text.secondary">
                                                    商品详情:
                                                </Typography>
                                                {order.productList.slice(0, 3).map((product: any, idx: number) => (
                                                    <Typography key={idx} variant="body2" sx={{ ml: 1 }}>
                                                        • {product.name || 'N/A'} - ¥{product.price || 0}
                                                    </Typography>
                                                ))}
                                                {order.productList.length > 3 && (
                                                    <Typography variant="body2" sx={{ ml: 1 }}>
                                                        ... 共 {order.productList.length} 件商品
                                                    </Typography>
                                                )}
                                            </Box>
                                        )}
                                        <Typography variant="body2" color="text.secondary">
                                            下单时间: {order.orderTime ? new Date(order.orderTime).toLocaleString() : 'N/A'}
                                        </Typography>
                                        <Chip
                                            label={order.orderStatus || '未知状态'}
                                            color={
                                                String(order.orderStatus) === OrderStatus.completed || String(order.orderStatus) === '已完成'
                                                    ? 'success'
                                                    : String(order.orderStatus) === OrderStatus.delivering || String(order.orderStatus) === '正在配送'
                                                        ? 'warning'
                                                        : 'primary'
                                            }
                                            sx={{ mt: 1 }}
                                        />
                                    </CardContent>
                                </Card>
                            </Grid>
                        ))}
                    </Grid>
                )}
            </TabPanel>

            {/* 购物车对话框 */}
            <Dialog open={cartOpen} onClose={() => setCartOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>购物车</DialogTitle>
                <DialogContent>
                    {cart.length === 0 ? (
                        <Typography>购物车为空</Typography>
                    ) : (
                        <>
                            <List>
                                {cart.map((item, index) => (
                                    <ListItem key={index}>
                                        <ListItemText
                                            primary={item.product.name}
                                            secondary={`¥${item.product.price.toFixed(2)} × ${item.quantity}`}
                                        />
                                        <IconButton onClick={() => removeFromCart(item.product.productID)}>
                                            <Remove />
                                        </IconButton>
                                        <Typography sx={{ mx: 1 }}>{item.quantity}</Typography>
                                        <IconButton onClick={() => addToCart(item.product)}>
                                            <Add />
                                        </IconButton>
                                    </ListItem>
                                ))}
                                <Divider />
                                <ListItem>
                                    <ListItemText
                                        primary={<Typography variant="h6">总计: ¥{getTotalPrice().toFixed(2)}</Typography>}
                                    />
                                </ListItem>
                            </List>

                            {/* 配送地址输入 */}
                            <TextField
                                fullWidth
                                label="配送地址"
                                variant="outlined"
                                value={deliveryAddress}
                                onChange={(e) => setDeliveryAddress(e.target.value)}
                                placeholder="请输入详细的配送地址"
                                sx={{ mt: 2 }}
                                multiline
                                rows={2}
                                required
                            />
                        </>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setCartOpen(false)}>关闭</Button>
                    {cart.length > 0 && (
                        <Button
                            variant="contained"
                            onClick={handleCheckout}
                            disabled={loading}
                        >
                            {loading ? <CircularProgress size={20} /> : '结算'}
                        </Button>
                    )}
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default OrderPage;
