#include "NetworkHelper.h"

NetworkHelper::NetworkHelper(QApplication* app, QStringList& cryptos) {
    manager = new QNetworkAccessManager(app);
    this->cryptos = cryptos;
}

// Crates the URL for requesting price info and makes a get request
void NetworkHelper::PopulateTable() const {
    auto status = connect(manager, &QNetworkAccessManager::finished, this, &NetworkHelper::ReplyFinished);
    qDebug() << "Connection status:" << status;

    // Create the URL
    QString url = "https://api.coingecko.com/api/v3/simple/price?ids=";
    for (int i = 0; i < cryptos.size(); i++) {
        url = url + cryptos.at(i) + ",";
    }
    url = url.left(url.size() - 1);
    url = url + "&vs_currencies=usd,eur,gbp";

    // Make the GET request
    manager->get(QNetworkRequest(QUrl(url)));
}

// Read JSON response and creates the table
void NetworkHelper::ReplyFinished(QNetworkReply *reply) {
    // Convert response to usable JSON object
    QByteArray response_data = reply->readAll();
    QJsonDocument json = QJsonDocument::fromJson(response_data);
    QJsonObject prices = json.object();

    // Delete reply
    reply->deleteLater();

    // Create the table
    QTableWidget* tableWidget = new QTableWidget(cryptos.size(), 3);

    // Add all prices
    for (int i = 0; i < cryptos.size(); i++) {
        QTableWidgetItem *usdPrice = new QTableWidgetItem(QString::number(prices[cryptos.at(i)].toObject()["usd"].toDouble()));
        tableWidget->setItem(i, 0, usdPrice);
        QTableWidgetItem *eurPrice = new QTableWidgetItem(QString::number(prices[cryptos.at(i)].toObject()["eur"].toDouble()));
        tableWidget->setItem(i, 1, eurPrice);
        QTableWidgetItem *gbpPrice = new QTableWidgetItem(QString::number(prices[cryptos.at(i)].toObject()["gbp"].toDouble()));
        tableWidget->setItem(i, 2, gbpPrice);
    }

    // Capitalize first letters
    for (int i = 0; i < cryptos.size(); i++) {
        cryptos[i] = cryptos[i].replace(0, 1, cryptos[i][0].toUpper());
    }

    // Set headers
    tableWidget->setHorizontalHeaderLabels({ "USD", "EUR", "GBP" });
    tableWidget->setVerticalHeaderLabels(cryptos);

    // Show table
    tableWidget->show();
}