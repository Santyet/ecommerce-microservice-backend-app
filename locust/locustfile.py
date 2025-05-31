from locust import HttpUser, task, between

class MyUser(HttpUser):
    wait_time = between(1, 5)

    host = "http://localhost:8080"

    @task
    def getUsers(self):
        self.client.get("/user-service/api/users/")

    @task
    def getShippings(self):
        self.client.get("/shipping-service/api/favourites/")
    
    @task
    def getPayments(self):
        self.client.get("/payment-service/api/payments/")

    @task
    def getOrders(self):
        self.client.get("/order-service/api/orders/")

    @task
    def getProducts(self):
        self.client.get("/product-service/api/products/")
    
    