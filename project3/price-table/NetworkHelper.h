#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QApplication>
#include <QTableWidget>
#include <QTableWidgetItem>
#include <QStringList>
#include <QJsonDocument>
#include <QJsonObject>

class NetworkHelper : public QObject {
Q_OBJECT
public:
    explicit NetworkHelper(QApplication* application, QStringList& cryptos);
    void PopulateTable() const;
    void ReplyFinished(QNetworkReply *reply);

    QNetworkAccessManager *manager;
    QStringList cryptos;
};
