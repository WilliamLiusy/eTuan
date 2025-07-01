import React, { useState } from 'react';
import { Box, Card, CardContent, TextField, Button, Typography, Alert, CircularProgress } from '@mui/material';
import { UserLogin } from '../Plugins/UserCenter/APIs/UserLogin';
import { GetUserInfoByToken } from '../Plugins/UserCenter/APIs/GetUserInfoByToken';
import { setUserToken } from '../Globals/GlobalStore';

const LoginPage: React.FC = () => {
    const [account, setAccount] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleLogin = async () => {
        if (!account.trim() || !password.trim()) {
            setError('请输入账号和密码');
            return;
        }

        setLoading(true);
        setError('');

        try {
            const loginMessage = new UserLogin(account.trim(), password);

            loginMessage.send(
                (token: string) => {
                    // 登录成功，保存token
                    setUserToken(token);

                    // 获取用户信息以确定跳转页面
                    const getUserInfo = new GetUserInfoByToken(token);
                    getUserInfo.send(
                        (userInfoStr: string) => {
                            try {
                                const userInfo = JSON.parse(userInfoStr);

                                // 根据用户类型跳转到对应页面
                                switch (userInfo.userType) {
                                    case 'customer':
                                        window.location.hash = '#/order';
                                        break;
                                    case 'merchant':
                                        window.location.hash = '#/merchant';
                                        break;
                                    case 'rider':
                                        window.location.hash = '#/rider';
                                        break;
                                    default:
                                        window.location.hash = '#/order';
                                }
                            } catch (parseError) {
                                console.error('解析用户信息失败:', parseError);
                                window.location.hash = '#/order';
                            }
                            setLoading(false);
                        },
                        (error: any) => {
                            console.error('获取用户信息失败:', error);
                            // 获取用户信息失败时，默认跳转到订单页面
                            window.location.hash = '#/order';
                            setLoading(false);
                        }
                    );
                },
                (error: any) => {
                    setError(error?.message || '登录失败，请检查账号密码');
                    setLoading(false);
                }
            );
        } catch (err) {
            setError('网络错误，请稍后重试');
            console.error('Login error:', err);
            setLoading(false);
        }
    };

    const handleKeyPress = (event: React.KeyboardEvent) => {
        if (event.key === 'Enter') {
            handleLogin();
        }
    };

    return (
        <Box
            sx={{
                minHeight: '100vh',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                padding: 2
            }}
        >
            <Card sx={{ maxWidth: 400, width: '100%' }}>
                <CardContent sx={{ p: 4 }}>
                    <Typography variant="h4" component="h1" gutterBottom align="center" color="primary">
                        外卖系统登录
                    </Typography>

                    {error && (
                        <Alert severity="error" sx={{ mb: 2 }}>
                            {error}
                        </Alert>
                    )}

                    <TextField
                        fullWidth
                        label="账号"
                        variant="outlined"
                        value={account}
                        onChange={(e) => setAccount(e.target.value)}
                        onKeyPress={handleKeyPress}
                        sx={{ mb: 2 }}
                        disabled={loading}
                    />

                    <TextField
                        fullWidth
                        label="密码"
                        type="password"
                        variant="outlined"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        onKeyPress={handleKeyPress}
                        sx={{ mb: 3 }}
                        disabled={loading}
                    />

                    <Button
                        fullWidth
                        variant="contained"
                        size="large"
                        onClick={handleLogin}
                        disabled={loading}
                        sx={{ mb: 2 }}
                    >
                        {loading ? <CircularProgress size={24} /> : '登录'}
                    </Button>

                    <Box textAlign="center">
                        <Button
                            color="primary"
                            onClick={() => window.location.hash = '#/register'}
                            disabled={loading}
                        >
                            还没有账号？立即注册
                        </Button>
                    </Box>
                </CardContent>
            </Card>
        </Box>
    );
};

export default LoginPage;
