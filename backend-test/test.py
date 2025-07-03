import pytest
import requests
import random

# ----------------------
# 常量定义
# ----------------------

USER_SERVICE = "10010"
ORDER_SERVICE = "10011"
PRODUCT_SERVICE = "10012"
DISPATCHER_SERVICE = "10013"
PASSWORD = "abcdefgh"

# 用户类型常量（对应后端中文）
CUSTOMER = "顾客"
MERCHANT = "商家"
RIDER = "骑手"
USER_TYPES = [CUSTOMER, MERCHANT, RIDER]

# 骑手状态常量
RIDESTATUS_IDLE = "空闲"
RIDESTATUS_DELIVERING = "配送中"
RIDESTATUS_OFFDUTY = "下班"  # 默认状态

VALID_RIDESTATUS = [RIDESTATUS_IDLE, RIDESTATUS_DELIVERING, RIDESTATUS_OFFDUTY]

# 默认用户状态映射
DEFAULT_STATUS_MAP = {
    CUSTOMER: RIDESTATUS_OFFDUTY,
    MERCHANT: RIDESTATUS_OFFDUTY,
    RIDER: RIDESTATUS_OFFDUTY  # 所有骑手默认状态均为 “下班”
}

# 默认联系方式
DEFAULT_CONTACT_NUMBER = "13800000000"

# ----------------------
# 工具函数
# ----------------------

def gen_url(service: str, call_name: str) -> str:  # 参数名改为 call_name
    return f"http://localhost:{service}/api/{call_name}"

def call_api(service: str, call_name: str, **args) -> requests.Response:
    return requests.post(gen_url(service, call_name), json=args, verify=False)

def gen_name() -> str:
    return str(random.randint(0, 10**8))

# ----------------------
# 测试用例
# ----------------------

def test_customer_register():
    name = gen_name()
    user_type = CUSTOMER
    address = ""

    response = call_api(USER_SERVICE, "UserRegister",
                        name=name,
                        contactNumber=DEFAULT_CONTACT_NUMBER,
                        password=PASSWORD,
                        userType=user_type,
                        address=address)

    assert response.status_code == 200, f"Expected status code 200, got {response.status_code}"
    token = response.json()  # 👈 此时 data 就是 token 字符串
    assert isinstance(token, str) and len(token) > 0, "Expected non-empty token string"
    print("✅ 顾客注册成功")


def test_merchant_register_with_address():
    name = gen_name()
    user_type = MERCHANT
    address = "上海市南京东路1号"

    response = call_api(USER_SERVICE, "UserRegister",
                        name=name,
                        contactNumber=DEFAULT_CONTACT_NUMBER,
                        password=PASSWORD,
                        userType=user_type,
                        address=address)

    assert response.status_code == 200
    token = response.json()
    assert isinstance(token, str) and len(token) > 0
    print("✅ 商家注册成功")


def test_merchant_register_without_address_should_fail():
    name = gen_name()
    user_type = MERCHANT
    address = ""

    response = call_api(USER_SERVICE, "UserRegister",
                        name=name,
                        contactNumber=DEFAULT_CONTACT_NUMBER,
                        password=PASSWORD,
                        userType=user_type,
                        address=address)

    assert response.status_code != 200
    print("✅ 商家未填写地址，注册失败（符合预期）")


def test_rider_register():
    name = gen_name()
    user_type = RIDER
    address = ""

    response = call_api(USER_SERVICE, "UserRegister",
                        name=name,
                        contactNumber=DEFAULT_CONTACT_NUMBER,
                        password=PASSWORD,
                        userType=user_type,
                        address=address)

    assert response.status_code == 200
    token = response.json()
    assert isinstance(token, str) and len(token) > 0
    print("✅ 骑手注册成功")

def register_user(name=None, user_type=CUSTOMER, password=PASSWORD, contact_number=DEFAULT_CONTACT_NUMBER, address=""):
    """
    注册一个新用户，并返回其 name 和 password。
    可选参数允许自定义注册内容。
    """
    if name is None:
        name = gen_name()

    response = call_api(USER_SERVICE, "UserRegister",
                        name=name,
                        contactNumber=contact_number,
                        password=password,
                        userType=user_type,
                        address=address)
    
    assert response.status_code == 200, f"注册失败：{response.text}"
    token = response.json()
    assert isinstance(token, str) and len(token) > 0, "注册返回的 token 异常"

    return {
        "name": name,
        "password": password
    }

def test_user_login_success():
    # 1. 注册用户
    user = register_user()
    name = user["name"]
    password = user["password"]

    # 2. 登录用户
    login_response = call_api(USER_SERVICE, "UserLogin",
                              name=name,
                              password=password)

    assert login_response.status_code == 200, f"Expected status code 200, got {login_response.status_code}"
    token = login_response.json()
    assert isinstance(token, str) and len(token) > 0, "Expected non-empty token string"
    print("✅ 用户登录成功")


def test_user_login_invalid_password():
    # 1. 注册用户
    user = register_user()
    name = user["name"]
    wrong_password = "wrong_password"

    # 2. 使用错误密码登录
    login_response = call_api(USER_SERVICE, "UserLogin",
                              name=name,
                              password=wrong_password)

    assert login_response.status_code != 200, "预期登录失败（密码错误）"
    print("✅ 登录失败（密码错误），符合预期")


def test_user_login_nonexistent_user():
    # 直接尝试登录一个未注册的用户名
    name = gen_name()
    password = PASSWORD

    response = call_api(USER_SERVICE, "UserLogin",
                        name=name,
                        password=password)

    assert response.status_code != 200, "预期登录失败（用户不存在）"
    print("✅ 登录失败（用户不存在），符合预期")

def get_user_info_by_token(token: str):
    response = call_api(USER_SERVICE, "GetUserInfoByToken", userToken=token)
    return response

def do_test_user_info(user_type, expected_address="", expected_status=None):
    """
    测试获取用户信息接口的通用流程
    :param user_type: 用户类型（顾客 / 商家 / 骑手）
    :param expected_address: 预期地址（商家必须填写）
    :param expected_status: 预期状态（骑手默认为下班）
    """

    # 1. 注册用户
    user = register_user(user_type=user_type, address=expected_address)
    name = user["name"]
    password = user["password"]

    # 2. 登录获取 token
    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    token = login_response.json()
    assert isinstance(token, str) and len(token) > 0

    # 3. 获取用户信息
    info_response = get_user_info_by_token(token)
    assert info_response.status_code == 200
    user_info = info_response.json()

    # 4. 验证字段是否存在
    assert "userID" in user_info
    assert "name" in user_info
    assert "contactNumber" in user_info
    assert "userType" in user_info
    assert "address" in user_info
    assert "status" in user_info
    assert "createTime" in user_info

    # 5. 验证字段值
    assert user_info["name"] == name
    assert user_info["contactNumber"] == DEFAULT_CONTACT_NUMBER
    assert user_info["userType"] == user_type

    if expected_address != '':
        assert user_info["address"] == expected_address
    else:
        assert user_info["address"] is None or user_info["address"] == "" or user_info["address"] == "None"

    if expected_status is not None:
        assert user_info["status"] == expected_status
    else:
        assert user_info["status"] == RIDESTATUS_OFFDUTY

    print(f"✅ {user_type} UserInfo 获取成功，字段验证通过")

def test_get_user_info_for_customer():
    do_test_user_info(user_type=CUSTOMER,
        expected_status=RIDESTATUS_OFFDUTY  # 所有骑手默认状态是“下班”
                      )

def test_get_user_info_for_merchant():
    do_test_user_info(
        user_type=MERCHANT,
        expected_address="上海市南京东路1号",
        expected_status=RIDESTATUS_OFFDUTY  # 所有骑手默认状态是“下班”
    )

def test_get_user_info_for_rider():
    do_test_user_info(
        user_type=RIDER,
        expected_status=RIDESTATUS_OFFDUTY  # 所有骑手默认状态是“下班”
    )

def update_rider_status(token: str, new_status: str):
    response = call_api(USER_SERVICE, "UpdateStatus", userToken=token, newStatus=new_status)
    return response

def test_update_rider_status_success():
    # 1. 注册并登录一个骑手
    rider = register_user(user_type=RIDER)
    name = rider["name"]
    password = rider["password"]

    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    token = login_response.json()

    # 2. 更新状态为“空闲”
    update_response = update_rider_status(token, RIDESTATUS_IDLE)
    assert update_response.status_code == 200
    result = update_response.json()
    assert result != "failure"

    # 3. 获取用户信息验证状态更新
    info_response = get_user_info_by_token(token)
    assert info_response.status_code == 200
    user_info = info_response.json()
    assert user_info["status"] == RIDESTATUS_IDLE

    print("✅ 骑手状态更新成功")


def test_update_rider_status_invalid_value():
    # 1. 注册并登录一个骑手
    rider = register_user(user_type=RIDER)
    name = rider["name"]
    password = rider["password"]

    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    token = login_response.json()

    # 2. 使用非法状态尝试更新
    invalid_status = "未知状态"
    update_response = update_rider_status(token, invalid_status)
    assert update_response.status_code != 200 or update_response.json() == "failure"

    print("✅ 非法状态更新被拒绝")


def test_update_rider_status_non_rider_forbidden():
    # 1. 注册并登录一个顾客
    customer = register_user(user_type=CUSTOMER)
    name = customer["name"]
    password = customer["password"]

    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    token = login_response.json()

    # 2. 尝试更新状态（应失败）
    update_response = update_rider_status(token, RIDESTATUS_IDLE)
    assert update_response.status_code != 200 or update_response.json() == "failure"

    print("✅ 非骑手用户无法更新骑手状态")

def get_all_idle_riders():
    response = call_api(USER_SERVICE, "GetAllIdleRiders")
    return response

def test_get_all_idle_riders_success():
    # 1. 注册并登录一个骑手
    rider = register_user(user_type=RIDER)
    name = rider["name"]
    password = rider["password"]

    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    token = login_response.json()

    # 2. 更新骑手状态为 “空闲”
    update_response = update_rider_status(token, RIDESTATUS_IDLE)
    assert update_response.status_code == 200
    assert update_response.json() != "failure"

    # 3. 调用 GetAllIdleRiders 接口获取所有空闲骑手
    response = get_all_idle_riders()
    assert response.status_code == 200

    idle_riders = response.json()

    # 4. 验证返回类型是 list
    assert isinstance(idle_riders, list), "Expected a list of idle riders"

    # 5. 如果有数据，验证每项包含 UserInfo 基本字段
    if len(idle_riders) > 0:
        sample = idle_riders[0]

        assert "userID" in sample
        assert "name" in sample
        assert "contactNumber" in sample
        assert "userType" in sample
        assert "address" in sample
        assert "status" in sample
        assert "createTime" in sample

        assert sample["userType"] == RIDER
        assert sample["status"] == RIDESTATUS_IDLE

    print("✅ 获取空闲骑手列表成功，结构验证通过")

def get_all_merchants():
    response = call_api(USER_SERVICE, "GetAllMerchants")
    return response

def test_get_all_merchants_success():
    # 1. 注册一个商家
    merchant = register_user(user_type=MERCHANT, address="上海市南京东路1号")
    name = merchant["name"]
    password = merchant["password"]

    # 登录获取 token（只是为了触发商家数据存在）
    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200

    # 2. 调用 GetAllMerchants 接口获取所有商家
    response = get_all_merchants()
    assert response.status_code == 200

    merchants = response.json()

    # 3. 验证返回类型是 list
    assert isinstance(merchants, list), "Expected a list of merchants"

    # 4. 如果有数据，验证每项包含 UserInfo 基本字段
    if len(merchants) > 0:
        sample = merchants[0]

        assert "userID" in sample
        assert "name" in sample
        assert "contactNumber" in sample
        assert "userType" in sample
        assert "address" in sample
        assert "status" in sample
        assert "createTime" in sample

        assert sample["userType"] == MERCHANT
        assert sample["address"] is not None and sample["address"] != ""

    print("✅ 获取商家列表成功，结构验证通过")

def add_product(merchant_token, name, price, description):
    response = call_api(PRODUCT_SERVICE, "MerchantAddProductMessage",
                        merchantToken=merchant_token,
                        name=name,
                        price=price,
                        description=description)
    return response

def test_merchant_add_product_success():
    # 1. 注册一个商家
    merchant = register_user(user_type=MERCHANT, address="上海市南京东路1号")
    name = merchant["name"]
    password = merchant["password"]

    # 2. 登录获取 token
    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    token = login_response.json()
    assert isinstance(token, str) and len(token) > 0

    # 3. 准备商品信息
    product_name = "招牌奶茶"
    product_price = 15.9
    product_description = "本店特色饮品，每日现做"

    # 4. 添加商品
    response = add_product(token, product_name, product_price, product_description)
    assert response.status_code == 200
    result = response.json()
    assert result == "Success", f"Expected 'Success', got {result}"

    print("✅ 商家添加商品成功")

def test_merchant_add_product_with_empty_name_should_fail():
    # 1. 注册并登录商家
    merchant = register_user(user_type=MERCHANT, address="上海市南京东路1号")
    name = merchant["name"]
    password = merchant["password"]

    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    token = login_response.json()

    # 2. 尝试添加商品，名称为空
    response = add_product(token, "", 15.9, "无名称的商品")
    assert response.status_code == 200
    result = response.json()
    assert result == "Failure", f"Expected 'Failure', got {result}"

    print("✅ 空名称添加商品失败（预期行为）")


def test_merchant_add_product_with_negative_price_should_fail():
    # 1. 登录商家
    merchant = register_user(user_type=MERCHANT, address="上海市南京东路1号")
    name = merchant["name"]
    password = merchant["password"]

    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    token = login_response.json()

    # 2. 尝试添加商品，价格为负数
    response = add_product(token, "招牌奶茶", -1, "价格非法")
    assert response.status_code == 200
    result = response.json()
    assert result == "Failure", f"Expected 'Failure', got {result}"

    print("✅ 负价格添加商品失败（预期行为）")

def test_merchant_add_duplicate_product_should_fail():
    # 1. 注册一个商家
    merchant = register_user(user_type=MERCHANT, address="上海市南京东路1号")
    name = merchant["name"]
    password = merchant["password"]

    # 2. 登录获取 token
    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    token = login_response.json()
    assert isinstance(token, str) and len(token) > 0

    # 3. 添加第一个商品
    product_name = "招牌奶茶"
    product_price = 15.9
    product_description = "本店特色饮品，每日现做"

    response = add_product(token, product_name, product_price, product_description)
    assert response.status_code == 200 and response.json() == "Success"

    # 4. 再次尝试添加相同名称的商品
    duplicate_response = add_product(token, product_name, product_price, product_description)
    assert duplicate_response.status_code == 200
    result = duplicate_response.json()
    assert result == "Failure", f"Expected 'Failure', got {result}"

    print("✅ 同一商家添加重名商品失败，符合预期")

def fetch_products_by_merchant_id(merchant_id: str):
    response = call_api(PRODUCT_SERVICE, "FetchProductsByMerchantIDMessage", merchantID=merchant_id)
    return response

def test_fetch_products_by_merchant_id_consistency():
    # 1. 注册一个商家
    merchant = register_user(user_type=MERCHANT, address="上海市南京东路1号")
    name = merchant["name"]
    password = merchant["password"]

    # 2. 登录获取 token
    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    token = login_response.json()
    assert isinstance(token, str) and len(token) > 0

    # 3. 获取 merchantID
    user_info_response = get_user_info_by_token(token)
    assert user_info_response.status_code == 200
    user_info = user_info_response.json()
    merchant_id = user_info["userID"]

    # 4. 准备并添加多个商品
    expected_products = [
        {
            "name": "招牌奶茶",
            "price": 15.9,
            "description": "每日现做"
        },
        {
            "name": "手工蛋糕",
            "price": 25.5,
            "description": "低糖健康"
        }
    ]

    added_products = []

    for product in expected_products:
        response = add_product(
            merchant_token=token,
            name=product["name"],
            price=product["price"],
            description=product["description"]
        )
        assert response.status_code == 200 and response.json() == "Success"
        added_products.append(product)

    # 5. 查询该商家的所有商品
    fetch_response = fetch_products_by_merchant_id(merchant_id)
    assert fetch_response.status_code == 200
    fetched_products = fetch_response.json()

    # 6. 验证数量一致
    assert len(fetched_products) == len(expected_products), \
        f"Expected {len(expected_products)} products, got {len(fetched_products)}"

    # 7. 验证每个商品字段一致（不校验 productID）
    for expected, actual in zip(expected_products, fetched_products):
        assert actual["name"] == expected["name"], \
            f"Expected name: {expected['name']}, got: {actual['name']}"
        assert abs(actual["price"] - expected["price"]) < 1e-6, \
            f"Expected price: {expected['price']}, got: {actual['price']}"
        assert actual["description"] == expected["description"], \
            f"Expected description: {expected['description']}, got: {actual['description']}"

    print("✅ 获取商家商品列表成功，且数据一致性验证通过")

def test_fetch_products_by_nonexistent_merchant_id():
    nonexistent_merchant_id = "nonexistent_merchant_id_123"

    response = fetch_products_by_merchant_id(nonexistent_merchant_id)
    assert response.status_code == 200

    products = response.json()
    assert isinstance(products, list), "Expected a list of products"
    assert len(products) == 0, "Expected empty list for nonexistent merchant"

    print("✅ 查询不存在的商家ID返回空列表，符合预期")

def fetch_product_by_name_and_merchant_id(merchant_id: str, name: str):
    response = call_api(PRODUCT_SERVICE, "FetchProductsByNameAndMerchantIDMessage",
                         merchantID=merchant_id, name=name)
    return response

def test_fetch_product_by_name_and_merchant_id_consistency():
    # 1. 注册一个商家
    merchant = register_user(user_type=MERCHANT, address="上海市南京东路1号")
    name = merchant["name"]
    password = merchant["password"]

    # 2. 登录获取 token
    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    token = login_response.json()
    assert isinstance(token, str) and len(token) > 0

    # 3. 获取 merchantID
    user_info_response = get_user_info_by_token(token)
    assert user_info_response.status_code == 200
    user_info = user_info_response.json()
    merchant_id = user_info["userID"]

    # 4. 添加一个商品并记录 productID
    expected_product = {
        "name": "招牌奶茶",
        "price": 15.9,
        "description": "每日现做"
    }

    add_response = add_product(
        merchant_token=token,
        name=expected_product["name"],
        price=expected_product["price"],
        description=expected_product["description"]
    )
    assert add_response.status_code == 200 and add_response.json() == "Success"

    # 假设添加商品接口会返回 productID（如果有的话），否则我们通过查询来获取
    # 这里假设商品已成功写入数据库，我们调用商品列表接口来获取 productID
    fetch_list_response = fetch_products_by_merchant_id(merchant_id)
    assert fetch_list_response.status_code == 200
    products = fetch_list_response.json()

    expected_product_with_id = None
    for p in products:
        if p["name"] == expected_product["name"]:
            expected_product_with_id = p
            break

    assert expected_product_with_id is not None, "无法找到刚刚添加的商品"

    # 5. 查询该商品
    fetch_response = fetch_product_by_name_and_merchant_id(merchant_id, expected_product["name"])
    assert fetch_response.status_code == 200
    fetched_product = fetch_response.json()

    # 6. 验证返回值不是 None
    assert fetched_product is not None and fetched_product != [], "Expected a product, got None"
    fetched_product = fetched_product[0]

    # 7. 验证字段一致性（包括 productID 和 merchantID）
    assert fetched_product["productID"] == expected_product_with_id["productID"], \
        f"Expected productID: {expected_product_with_id['productID']}, got: {fetched_product['productID']}"
    assert fetched_product["merchantID"] == expected_product_with_id["merchantID"], \
        f"Expected merchantID: {expected_product_with_id['merchantID']}, got: {fetched_product['merchantID']}"
    assert fetched_product["name"] == expected_product["name"], \
        f"Expected name: {expected_product['name']}, got: {fetched_product['name']}"
    assert abs(fetched_product["price"] - expected_product["price"]) < 1e-6, \
        f"Expected price: {expected_product['price']}, got: {fetched_product['price']}"
    assert fetched_product["description"] == expected_product["description"], \
        f"Expected description: {expected_product['description']}, got: {fetched_product['description']}"

    print("✅ 成功查询到商品，字段一致性验证通过")


def test_fetch_nonexistent_product_returns_none():
    # 1. 注册一个商家
    merchant = register_user(user_type=MERCHANT, address="上海市南京东路1号")
    name = merchant["name"]
    password = merchant["password"]

    # 2. 登录获取 token
    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    token = login_response.json()
    assert isinstance(token, str) and len(token) > 0

    # 3. 获取 merchantID
    user_info_response = get_user_info_by_token(token)
    assert user_info_response.status_code == 200
    user_info = user_info_response.json()
    merchant_id = user_info["userID"]

    # 4. 查询一个不存在的商品名称
    nonexistent_name = "不存在的商品"

    fetch_response = fetch_product_by_name_and_merchant_id(merchant_id, nonexistent_name)
    assert fetch_response.status_code == 200
    fetched_product = fetch_response.json()

    # 5. 验证返回值是 None
    assert fetched_product is None, f"Expected None, got {fetched_product}"

    print("✅ 查询不存在的商品返回 None，符合预期")


def test_fetch_product_from_other_merchant_returns_none():
    # 1. 注册第一个商家并添加商品
    merchant1 = register_user(user_type=MERCHANT, address="上海市南京东路1号")
    name1 = merchant1["name"]
    password1 = merchant1["password"]

    login_response1 = call_api(USER_SERVICE, "UserLogin", name=name1, password=password1)
    assert login_response1.status_code == 200
    token1 = login_response1.json()

    user_info_response1 = get_user_info_by_token(token1)
    assert user_info_response1.status_code == 200
    user_info1 = user_info_response1.json()
    merchant_id1 = user_info1["userID"]

    add_product(token1, "招牌奶茶", 15.9, "每日现做")

    # 2. 注册第二个商家
    merchant2 = register_user(user_type=MERCHANT, address="上海市南京东路2号")
    name2 = merchant2["name"]
    password2 = merchant2["password"]

    login_response2 = call_api(USER_SERVICE, "UserLogin", name=name2, password=password2)
    assert login_response2.status_code == 200
    token2 = login_response2.json()

    user_info_response2 = get_user_info_by_token(token2)
    assert user_info_response2.status_code == 200
    user_info2 = user_info_response2.json()
    merchant_id2 = user_info2["userID"]

    # 3. 使用商家2查询商家1的商品
    fetch_response = fetch_product_by_name_and_merchant_id(merchant_id2, "招牌奶茶")
    assert fetch_response.status_code == 200
    fetched_product = fetch_response.json()

    assert fetched_product is None, f"Expected None, got {fetched_product}"

    print("✅ 商家2无法查询到商家1的商品，符合预期")

def remove_product(merchant_token: str, name: str):
    """
    调用 ProductService 的 MerchantRemoveProductMessage 接口
    :param merchant_token: 商家的身份令牌
    :param name: 商品名称
    :return: Response 对象
    """
    return call_api(PRODUCT_SERVICE, "MerchantRemoveProductMessage",
                    merchantToken=merchant_token,
                    name=name)

def test_merchant_remove_product_success():
    # 1. 注册一个商家
    merchant = register_user(user_type=MERCHANT, address="上海市南京东路1号")
    name = merchant["name"]
    password = merchant["password"]

    # 2. 登录获取 token
    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    token = login_response.json()

    # 3. 添加一个商品
    product_name = "招牌奶茶"
    add_response = add_product(token, product_name, 15.9, "每日现做")
    assert add_response.status_code == 200 and add_response.json() == "Success"

    # 4. 删除该商品
    remove_response = remove_product(token, product_name)
    assert remove_response.status_code == 200
    result = remove_response.json()
    assert result == "Success", f"Expected 'Success', got {result}"

    # 5. 验证商品是否已删除
    user_info_response = get_user_info_by_token(token)
    assert user_info_response.status_code == 200
    merchant_id = user_info_response.json()["userID"]

    fetch_response = fetch_products_by_merchant_id(merchant_id)
    products = fetch_response.json()
    assert len(products) == 0, "商品未被正确删除"

    print("✅ 商家成功删除商品")


def test_merchant_remove_nonexistent_product():
    # 1. 注册一个商家
    merchant = register_user(user_type=MERCHANT, address="上海市南京东路1号")
    name = merchant["name"]
    password = merchant["password"]

    # 2. 登录获取 token
    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    token = login_response.json()

    # 3. 尝试删除一个不存在的商品
    nonexistent_name = "不存在的商品"
    remove_response = remove_product(token, nonexistent_name)
    assert remove_response.status_code == 200
    result = remove_response.json()
    assert result == "ProductNotFound", f"Expected 'ProductNotFound', got {result}"

    print("✅ 删除不存在的商品返回 ProductNotFound")


def test_remove_product_by_non_merchant_forbidden():
    # 1. 注册一个顾客
    customer = register_user(user_type=CUSTOMER)
    name = customer["name"]
    password = customer["password"]

    # 2. 登录获取 token
    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    token = login_response.json()

    # 3. 尝试删除商品（应失败）
    remove_response = remove_product(token, "招牌奶茶")
    assert remove_response.status_code == 400
    result = remove_response.json()
    assert result == "Unauthorized", f"Expected 'Unauthorized', got {result}"

    print("✅ 非商家用户无法删除商品")


def test_remove_product_with_invalid_token():
    invalid_token = "invalid_or_empty_token_123"

    # 1. 使用非法 token 删除商品
    remove_response = remove_product(invalid_token, "招牌奶茶")
    assert remove_response.status_code == 400
    result = remove_response.json()
    assert result == "Unauthorized", f"Expected 'Unauthorized', got {result}"

    print("✅ 使用非法 token 删除商品失败")

def create_order(customer_token, merchant_id, product_list, destination_address):
    """
    调用 CreateOrder 接口
    :param customer_token: 顾客身份令牌
    :param merchant_id: 商家ID
    :param product_list: 商品信息列表（List[ProductInfo]）
    :param destination_address: 送达地址
    :return: Response 对象
    """
    return call_api(ORDER_SERVICE, "CreateOrder",
                    customerToken=customer_token,
                    merchantID=merchant_id,
                    productList=product_list,
                    destinationAddress=destination_address)

def test_create_order_success():
    # 1. 注册并登录顾客
    customer = register_user(user_type=CUSTOMER)
    name = customer["name"]
    password = customer["password"]
    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    customer_token = login_response.json()

    # 2. 注册并登录商家
    merchant = register_user(user_type=MERCHANT, address="上海市南京东路1号")
    merchant_name = merchant["name"]
    merchant_password = merchant["password"]
    merchant_login_response = call_api(USER_SERVICE, "UserLogin", name=merchant_name, password=merchant_password)
    assert merchant_login_response.status_code == 200
    merchant_token = merchant_login_response.json()

    # 获取商家ID
    merchant_info = get_user_info_by_token(merchant_token).json()
    merchant_id = merchant_info["userID"]

    # 3. 添加商品到商家
    product_name = "招牌奶茶"
    add_product(merchant_token, product_name, 15.9, "每日现做")
    fetch_response = fetch_products_by_merchant_id(merchant_id)
    assert fetch_response.status_code == 200
    products = fetch_response.json()
    assert len(products) >= 1
    product_info = products[0]

    # 4. 构建订单商品列表
    product_list = [{
        "productID": product_info["productID"],
        "merchantID": product_info["merchantID"],
        "name": product_info["name"],
        "price": product_info["price"],
        "description": product_info["description"]
    }]

    # 5. 创建订单
    destination_address = "上海市人民广场B座"
    order_response = create_order(customer_token, merchant_id, product_list, destination_address)
    assert order_response.status_code == 200
    order_result = order_response.json()
    assert isinstance(order_result, str) and len(order_result) > 0, "Expected non-empty order ID"

    print("✅ 顾客成功创建订单")

def test_create_order_with_empty_product_list_should_fail():
    # 1. 注册并登录顾客
    customer = register_user(user_type=CUSTOMER)
    name = customer["name"]
    password = customer["password"]
    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    customer_token = login_response.json()

    # 2. 注册并登录商家
    merchant = register_user(user_type=MERCHANT, address="上海市南京东路1号")
    merchant_name = merchant["name"]
    merchant_password = merchant["password"]
    merchant_login_response = call_api(USER_SERVICE, "UserLogin", name=merchant_name, password=merchant_password)
    assert merchant_login_response.status_code == 200
    merchant_token = merchant_login_response.json()

    # 获取商家ID
    merchant_info = get_user_info_by_token(merchant_token).json()
    merchant_id = merchant_info["userID"]

    # 3. 商品列表为空
    product_list = []

    # 4. 尝试创建订单
    destination_address = "上海市人民广场B座"
    order_response = create_order(customer_token, merchant_id, product_list, destination_address)
    assert order_response.status_code == 400

    print("✅ 商品列表为空时创建订单失败（预期行为）")

def test_create_order_with_nonexistent_merchant_id_should_fail():
    # 1. 注册并登录顾客
    customer = register_user(user_type=CUSTOMER)
    name = customer["name"]
    password = customer["password"]
    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    customer_token = login_response.json()

    # 2. 使用一个不存在的商家ID
    nonexistent_merchant_id = "nonexistent_merchant_id_123"

    # 3. 构造空商品列表
    product_list = []

    # 4. 尝试创建订单
    destination_address = "上海市人民广场B座"
    order_response = create_order(customer_token, nonexistent_merchant_id, product_list, destination_address)
    assert order_response.status_code == 400

    print("✅ 使用不存在的商家ID创建订单失败（预期行为）")

def test_create_order_by_non_customer_should_fail():
    # 1. 注册并登录骑手
    rider = register_user(user_type=RIDER)
    name = rider["name"]
    password = rider["password"]
    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    rider_token = login_response.json()

    # 2. 注册并登录商家
    merchant = register_user(user_type=MERCHANT, address="上海市南京东路1号")
    merchant_name = merchant["name"]
    merchant_password = merchant["password"]
    merchant_login_response = call_api(USER_SERVICE, "UserLogin", name=merchant_name, password=merchant_password)
    assert merchant_login_response.status_code == 200
    merchant_token = merchant_login_response.json()

    # 获取商家ID
    merchant_info = get_user_info_by_token(merchant_token).json()
    merchant_id = merchant_info["userID"]

    # 3. 构造商品列表
    product_list = []

    # 4. 骑手尝试创建订单（应失败）
    destination_address = "上海市人民广场B座"
    order_response = create_order(rider_token, merchant_id, product_list, destination_address)
    assert order_response.status_code == 400

    print("✅ 非顾客用户无法创建订单（预期行为）")

def get_order_details(order_id: str):
    """
    调用 GetOrderDetails 接口
    :param order_id: 订单ID
    :return: Response 对象
    """
    return call_api(ORDER_SERVICE, "GetOrderDetails", orderID=order_id)

def test_get_order_details_success():
    # 1. 注册并登录顾客
    customer = register_user(user_type=CUSTOMER)
    name = customer["name"]
    password = customer["password"]
    login_response = call_api(USER_SERVICE, "UserLogin", name=name, password=password)
    assert login_response.status_code == 200
    customer_token = login_response.json()

    # 2. 注册并登录商家
    merchant = register_user(user_type=MERCHANT, address="上海市南京东路1号")
    merchant_name = merchant["name"]
    merchant_password = merchant["password"]
    merchant_login_response = call_api(USER_SERVICE, "UserLogin", name=merchant_name, password=merchant_password)
    assert merchant_login_response.status_code == 200
    merchant_token = merchant_login_response.json()

    # 获取商家ID
    merchant_info = get_user_info_by_token(merchant_token).json()
    merchant_id = merchant_info["userID"]

    # 3. 添加商品到商家
    product_name = "招牌奶茶"
    add_product(merchant_token, product_name, 15.9, "每日现做")
    fetch_response = fetch_products_by_merchant_id(merchant_id)
    products = fetch_response.json()
    product_info = products[0]

    # 4. 构建订单商品列表
    product_list = [{
        "productID": product_info["productID"],
        "merchantID": product_info["merchantID"],
        "name": product_info["name"],
        "price": product_info["price"],
        "description": product_info["description"]
    }]

    # 5. 创建订单
    destination_address = "上海市人民广场B座"
    create_order_response = create_order(customer_token=customer_token,
                                     merchant_id=merchant_id,
                                     product_list=product_list,
                                     destination_address=destination_address)
    assert create_order_response.status_code == 200
    order_id = create_order_response.json()
    assert isinstance(order_id, str) and len(order_id) > 0

    # 6. 获取订单详情
    get_details_response = get_order_details(order_id)
    assert get_details_response.status_code == 200
    order_info = get_details_response.json()

    # 7. 验证字段是否存在
    assert "orderID" in order_info
    assert "customerID" in order_info
    assert "merchantID" in order_info
    assert "riderID" in order_info
    assert "productList" in order_info
    assert "destinationAddress" in order_info
    assert "orderStatus" in order_info
    assert "orderTime" in order_info

    # 8. 验证字段值
    assert order_info["orderID"] == order_id
    assert order_info["customerID"] is not None
    assert order_info["merchantID"] == merchant_id
    assert order_info["riderID"] is None or order_info["riderID"] == ""
    assert order_info["destinationAddress"] == destination_address
    assert order_info["orderStatus"] == "等待出餐"

    # 9. 验证商品信息一致性
    fetched_product = order_info["productList"][0]
    assert fetched_product["productID"] == product_list[0]["productID"]
    assert fetched_product["name"] == product_list[0]["name"]
    assert abs(fetched_product["price"] - product_list[0]["price"]) < 1e-6
    assert fetched_product["description"] == product_list[0]["description"]

    print("✅ 顾客成功获取订单详情，字段验证通过")

def test_get_order_details_with_invalid_order_id_should_fail():
    invalid_order_id = "invalid_order_id_123"
    response = get_order_details(invalid_order_id)
    assert response.status_code != 200 or response.json() == "failure"

    print("✅ 使用无效订单ID查询返回错误（预期行为）")