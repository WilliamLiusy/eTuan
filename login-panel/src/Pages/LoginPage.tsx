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
                    // 登录成功，处理token（移除可能的多余引号）
                    let cleanToken = token;
                    try {
                        // 如果token是JSON字符串，解析它
                        cleanToken = JSON.parse(token);
                    } catch {
                        // 如果不是JSON字符串，直接使用原值
                        cleanToken = token;
                    }

                    setUserToken(cleanToken);

                    // 获取用户信息以确定跳转页面
                    const getUserInfo = new GetUserInfoByToken(cleanToken);
                    getUserInfo.send(
                        (userInfoStr: string) => {
                            try {
                                const userInfo = JSON.parse(userInfoStr);
                                console.log('获取用户信息成功:', userInfo.userType);
                                switch (userInfo.userType) {
                                    case '顾客':
                                        window.location.hash = '#/order';
                                        break;
                                    case '商家':
                                        window.location.hash = '#/merchant';
                                        break;
                                    case '骑手':
                                        window.location.hash = '#/rider';
                                        break;
                                    default:
                                        {
                                            setError('获取用户信息失败');
                                            window.location.hash = '#/order';
                                        }
                                }
                            } catch (parseError) {
                                console.error('解析用户信息失败:', parseError);
                                window.location.hash = '#/order';
                            }
                            setLoading(false);
                        },
                        (error: any) => {
                            console.error('获取用户信息失败:', error);
                            window.location.hash = '#/login';
                            setError('获取用户信息失败');
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
