import React from 'react';
import { render } from 'react-dom';
import { HashRouter, Route, Switch } from 'react-router-dom';
import LoginPage from './Pages/LoginPage';
import RegisterPage from './Pages/RegisterPage';
import OrderPage from './Pages/OrderPage';
import MerchantPage from './Pages/MerchantPage';
import RiderPage from './Pages/RiderPage';

const Layout = () => {
    return (
        <HashRouter>
            <Switch>
                <Route path="/" exact component={LoginPage} />
                <Route path="/login" exact component={LoginPage} />
                <Route path="/register" exact component={RegisterPage} />
                <Route path="/order" exact component={OrderPage} />
                <Route path="/merchant" exact component={MerchantPage} />
                <Route path="/rider" exact component={RiderPage} />
            </Switch>
        </HashRouter>
    );
};

render(<Layout />, document.getElementById('root'));
