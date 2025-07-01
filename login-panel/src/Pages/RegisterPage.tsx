import React, { useState } from 'react';
import {
    Box, Card, CardContent, TextField, Button, Typography, Alert,
    CircularProgress, FormControl, InputLabel, Select, MenuItem
} from '@mui/material';
import { UserRegister } from '../Plugins/UserCenter/APIs/UserRegister';
import { UserType } from '../Plugins/UserCenter/Objects/UserType';
import { setUserToken } from '../Globals/GlobalStore';

const RegisterPage: React.FC = () => {
    const [name, setName] = useState('');
    const [contactNumber, setContactNumber] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [userType, setUserType] = useState<UserType>(UserType.customer);
    const [address, setAddress] = useState(''); // 商家专用
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    const handleRegister = async () => {
        // 验证输入
        if (!name.trim() || !contactNumber.trim() || !password.trim()) {
            setError('请填写所有必填项');
            return;
        }

        if (password !== confirmPassword) {
            setError('两次输入的密码不一致');
            return;
        }

        if (password.length < 6) {
            setError('密码长度至少6位');
            return;
        }

        if (userType === UserType.merchant && !address.trim()) {
            setError('商家必须填写地址信息');
            return;
        }

        setLoading(true);
        setError('');
        setSuccess('');

        try {
            const registerMessage = new UserRegister(
                name.trim(),
                contactNumber.trim(),
                password,
                userType
            );

            registerMessage.send(
                (token: string) => {
                    setSuccess('注册成功！正在跳转...');
                    setUserToken(token);

                    // 注册成功后根据用户类型跳转
                    setTimeout(() => {
                        switch (userType) {
                            case UserType.customer:
                                window.location.hash = '#/order';
                                break;
                            case UserType.merchant:
                                window.location.hash = '#/merchant';
                                break;
                            case UserType.rider:
                                window.location.hash = '#/rider';
                                break;
                            default:
                                window.location.hash = '#/order';
                        }
                    }, 1500);

                    setLoading(false);
                },
                (error: any) => {
                    setError(error?.message || '注册失败，请稍后重试');
                    setLoading(false);
                }
            );
        } catch (err) {
            setError('网络错误，请稍后重试');
            console.error('Register error:', err);
            setLoading(false);
        }
    };

    const handleKeyPress = (event: React.KeyboardEvent) => {
        if (event.key === 'Enter') {
            handleRegister();
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
            <Card sx={{ maxWidth: 500, width: '100%' }}>
                <CardContent sx={{ p: 4 }}>
                    <Typography variant="h4" component="h1" gutterBottom align="center" color="primary">
                        用户注册
                    </Typography>

                    {error && (
                        <Alert severity="error" sx={{ mb: 2 }}>
                            {error}
                        </Alert>
                    )}

                    {success && (
                        <Alert severity="success" sx={{ mb: 2 }}>
                            {success}
                        </Alert>
                    )}

                    <FormControl fullWidth sx={{ mb: 2 }}>
                        <InputLabel>用户类型</InputLabel>
                        <Select
                            value={userType}
                            label="用户类型"
                            onChange={(e) => setUserType(e.target.value as UserType)}
                            disabled={loading}
                        >
                            <MenuItem value={UserType.customer}>顾客</MenuItem>
                            <MenuItem value={UserType.merchant}>商家</MenuItem>
                            <MenuItem value={UserType.rider}>骑手</MenuItem>
                        </Select>
                    </FormControl>

                    <TextField
                        fullWidth
                        label="用户名"
                        variant="outlined"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        onKeyPress={handleKeyPress}
                        sx={{ mb: 2 }}
                        disabled={loading}
                    />

                    <TextField
                        fullWidth
                        label="联系方式"
                        variant="outlined"
                        value={contactNumber}
                        onChange={(e) => setContactNumber(e.target.value)}
                        onKeyPress={handleKeyPress}
                        sx={{ mb: 2 }}
                        disabled={loading}
                        placeholder="手机号或邮箱"
                    />

                    {userType === UserType.merchant && (
                        <TextField
                            fullWidth
                            label="商家地址"
                            variant="outlined"
                            value={address}
                            onChange={(e) => setAddress(e.target.value)}
                            onKeyPress={handleKeyPress}
                            sx={{ mb: 2 }}
                            disabled={loading}
                            placeholder="请输入商家地址"
                        />
                    )}

                    <TextField
                        fullWidth
                        label="密码"
                        type="password"
                        variant="outlined"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        onKeyPress={handleKeyPress}
                        sx={{ mb: 2 }}
                        disabled={loading}
                        placeholder="至少6位"
                    />

                    <TextField
                        fullWidth
                        label="确认密码"
                        type="password"
                        variant="outlined"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        onKeyPress={handleKeyPress}
                        sx={{ mb: 3 }}
                        disabled={loading}
                    />

                    <Button
                        fullWidth
                        variant="contained"
                        size="large"
                        onClick={handleRegister}
                        disabled={loading}
                        sx={{ mb: 2 }}
                    >
                        {loading ? <CircularProgress size={24} /> : '注册'}
                    </Button>

                    <Box textAlign="center">
                        <Button
                            color="primary"
                            onClick={() => window.location.hash = '#/login'}
                            disabled={loading}
                        >
                            已有账号？立即登录
                        </Button>
                    </Box>
                </CardContent>
            </Card>
        </Box>
    );
};

export default RegisterPage;
