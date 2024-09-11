import requests
import random
import time

while True:
    account_ids = [
    22214,
    11413,
    42422,
    52355,
    62654,
    10031,
    44044
    ]

    security = [
    "MMM",
    "AOS",
    "ABT",
    "ABBV",
    "CPB",
    "COF"
    ]

    side = [
        "Buy",
        "Sell"
    ]

    headers = {
        'Content-Type': 'application/json',
        'accept': 'application/json',
    }

    data = {"quantity":random.randint(1,1000),"accountId":random.choice(account_ids),"side":random.choice(side),"security":random.choice(security)}


    response = requests.post('http:/trade-service.default.svc.cluster.local:18092/trade/', headers=headers, json=data)
    print(response.text)
    time.sleep(1)
