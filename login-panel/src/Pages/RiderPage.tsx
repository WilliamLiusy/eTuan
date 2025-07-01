import React, { useState, useEffect } from 'react';
import {
    Box, AppBar, Toolbar, Typography, Button, Grid, Card, CardContent,
    CardActions, List, ListItem, ListItemText, Divider, Alert,
    CircularProgress, Tabs, Tab, Chip, Paper
} from '@mui/material';
import {
    DirectionsBike, Assignment, CheckCircle, Logout, LocalShipping
} from '@mui/icons-material';
import { GetUnassignedOrders } from '../Plugins/OrderService/APIs/GetUnassignedOrders';
import { UpdateRider } from '../Plugins/OrderService/APIs/UpdateRider';
import { UpdateOrderStatus } from '../Plugins/OrderService/APIs/UpdateOrderStatus';
import { QueryOrdersByUser } from '../Plugins/OrderService/APIs/QueryOrdersByUser';
import { GetUserInfoByToken } from '../Plugins/UserCenter/APIs/GetUserInfoByToken';
import { getUserToken, setUserToken } from '../Globals/GlobalStore';
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

const RiderPage: React.FC = () => {
    const [tabValue, setTabValue] = useState(0);
    const [availableOrders, setAvailableOrders] = useState<OrderInfo[]>([]);
    const [myOrders, setMyOrders] = useState<OrderInfo[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [userInfo, setUserInfo] = useState<any>(null);

    useEffect(() => {
        loadUserInfo();
        loadAvailableOrders();
        loadMyOrders();
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

    const loadAvailableOrders = async () => {
        setLoading(true);
        const getOrdersMsg = new GetUnassignedOrders();
        getOrdersMsg.send(
            (ordersStr: string) => {
                try {
                    const orderList = JSON.parse(ordersStr);
                    setAvailableOrders(orderList);
                } catch (err) {
                    setError('解析可接订单数据失败');
                } finally {
                    setLoading(false);
                }
            },
            (error: any) => {
                setError('获取可接订单失败');
                setLoading(false);
            }
        );
    };

    const loadMyOrders = async () => {
        const token = getUserToken();
        if (token) {
            const queryOrdersMsg = new QueryOrdersByUser(token);
            queryOrdersMsg.send(
                (ordersStr: string) => {
                    try {
                        const orderList = JSON.parse(ordersStr);
                        // 过滤出分配给当前骑手的订单
                        const myAssignedOrders = orderList.filter((order: OrderInfo) =>
                            order.riderID === userInfo?.id
                        );
                        setMyOrders(myAssignedOrders);
                    } catch (err) {
                        console.error('解析我的订单数据失败:', err);
                    }
                },
                (error: any) => {
                    console.error('获取我的订单失败:', error);
                }
            );
        }
    };

    const handleAcceptOrder = async (orderID: string) => {
        if (!userInfo?.id) {
            setError('用户信息不完整');
            return;
        }

        setLoading(true);

        try {
            const updateRiderMsg = new UpdateRider(orderID, userInfo.id);
            updateRiderMsg.send(
                (result: string) => {
                    setSuccess('接单成功！');
                    loadAvailableOrders(); // 重新加载可接订单
                    loadMyOrders(); // 重新加载我的订单
                    setLoading(false);
                },
                (error: any) => {
                    setError('接单失败：' + (error?.message || '未知错误'));
                    setLoading(false);
                }
            );
        } catch (err) {
            setError('接单失败，请稍后重试');
            setLoading(false);
        }
    };

    const handleUpdateOrderStatus = async (orderID: string, newStatus: OrderStatus) => {
        setLoading(true);

        try {
            const updateStatusMsg = new UpdateOrderStatus(orderID, newStatus);
            updateStatusMsg.send(
                (result: string) => {
                    setSuccess(`订单状态已更新为：${newStatus}`);
                    loadMyOrders(); // 重新加载我的订单
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

    const getStatusColor = (status: OrderStatus) => {
        switch (status) {
            case OrderStatus.waitingForAssign: return 'warning';
            case OrderStatus.waitingForDish: return 'info';
            case OrderStatus.delivering: return 'primary';
            case OrderStatus.completed: return 'success';
            default: return 'default';
        }
    };

    const getNextStatus = (currentStatus: OrderStatus): OrderStatus | null => {
        switch (currentStatus) {
            case OrderStatus.waitingForAssign: return OrderStatus.waitingForDish;
            case OrderStatus.waitingForDish: return OrderStatus.delivering;
            case OrderStatus.delivering: return OrderStatus.completed;
            default: return null;
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
                    <DirectionsBike sx={{ mr: 2 }} />
                    <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
                        外卖系统 - 骑手工作台
                    </Typography>
                    <Typography variant="body2" sx={{ mr: 2 }}>
                        欢迎，{userInfo?.name || '骑手'}
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

            {/* 统计信息 */}
            <Paper sx={{ p: 2, m: 2 }}>
                <Grid container spacing={3}>
                    <Grid item xs={12} sm={4}>
                        <Box textAlign="center">
                            <Typography variant="h4" color="primary">
                                {availableOrders.length}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                可接订单
                            </Typography>
                        </Box>
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <Box textAlign="center">
                            <Typography variant="h4" color="secondary">
                                {myOrders.filter(order => order.orderStatus !== '已完成').length}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                进行中订单
                            </Typography>
                        </Box>
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <Box textAlign="center">
                            <Typography variant="h4" color="success.main">
                                {myOrders.filter(order => order.orderStatus === '已完成').length}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                已完成订单
                            </Typography>
                        </Box>
                    </Grid>
                </Grid>
            </Paper>

            {/* 标签页 */}
            <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
                <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)}>
                    <Tab label="可接订单" icon={<Assignment />} />
                    <Tab label="我的订单" icon={<LocalShipping />} />
                </Tabs>
            </Box>

            {/* 可接订单 */}
            <TabPanel value={tabValue} index={0}>
                <Typography variant="h5" gutterBottom>
                    可接订单
                </Typography>

                {loading ? (
                    <Box display="flex" justifyContent="center" p={4}>
                        <CircularProgress />
                    </Box>
                ) : availableOrders.length === 0 ? (
                    <Typography color="text.secondary" align="center">
                        暂无可接订单
                    </Typography>
                ) : (
                    <Grid container spacing={3}>
                        {availableOrders.map((order) => (
                            <Grid item xs={12} md={6} key={order.orderID}>
                                <Card>
                                    <CardContent>
                                        <Typography variant="h6">
                                            订单 #{order.orderID.substring(0, 8)}...
                                        </Typography>
                                        <Typography color="text.secondary">
                                            商家: {order.merchantID}
                                        </Typography>
                                        <Typography>
                                            配送地址: {order.destinationAddress}
                                        </Typography>
                                        <Typography>
                                            商品数量: {order.productList?.length || 0}
                                        </Typography>
                                        <Chip
                                            label={order.orderStatus}
                                            color={getStatusColor(order.orderStatus) as any}
                                            sx={{ mt: 1 }}
                                        />
                                    </CardContent>
                                    <CardActions>
                                        <Button
                                            variant="contained"
                                            color="primary"
                                            onClick={() => handleAcceptOrder(order.orderID)}
                                            disabled={loading}
                                        >
                                            接单
                                        </Button>
                                    </CardActions>
                                </Card>
                            </Grid>
                        ))}
                    </Grid>
                )}
            </TabPanel>

            {/* 我的订单 */}
            <TabPanel value={tabValue} index={1}>
                <Typography variant="h5" gutterBottom>
                    我的订单
                </Typography>

                {myOrders.length === 0 ? (
                    <Typography color="text.secondary" align="center">
                        暂无订单
                    </Typography>
                ) : (
                    <Grid container spacing={3}>
                        {myOrders.map((order) => (
                            <Grid item xs={12} md={6} key={order.orderID}>
                                <Card>
                                    <CardContent>
                                        <Typography variant="h6">
                                            订单 #{order.orderID.substring(0, 8)}...
                                        </Typography>
                                        <Typography color="text.secondary">
                                            商家: {order.merchantID}
                                        </Typography>
                                        <Typography>
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
                                            color={getStatusColor(order.orderStatus) as any}
                                            sx={{ mt: 1 }}
                                        />
                                    </CardContent>
                                    <CardActions>
                                        {getNextStatus(order.orderStatus) && (
                                            <Button
                                                variant="contained"
                                                color="primary"
                                                onClick={() => handleUpdateOrderStatus(order.orderID, getNextStatus(order.orderStatus)!)}
                                                disabled={loading}
                                                startIcon={<CheckCircle />}
                                            >
                                                更新为：{getNextStatus(order.orderStatus)}
                                            </Button>
                                        )}
                                    </CardActions>
                                </Card>
                            </Grid>
                        ))}
                    </Grid>
                )}
            </TabPanel>
        </Box>
    );
};

export default RiderPage;
