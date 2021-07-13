#include <cstdlib>
#include <QFile>
#include <QString>

#include "NetworkHelper.h"

using namespace std;

// Atilla Türkmen
int main(int argc, char *argv[])
{
	// Get file path of crypto list from system variables
	const char* filePath = getenv("MYCRYPTOCONVERT");

	// Read names of crypto currencies from file to a QStringList
    QFile inFile(filePath);
    inFile.open(QIODevice::ReadOnly | QIODevice::Text);
    QStringList cryptos;
    int nofCryptos = 0;
    while (!inFile.atEnd()) {
        QString line = inFile.readLine();
        line = line.trimmed();
        line = line.toLower();
        nofCryptos++;
        cryptos.append(line);
    }

    // QT UI
    QApplication *app = new QApplication(argc, argv);
    NetworkHelper *helper = new NetworkHelper(app, cryptos);
    helper->PopulateTable();

    return QApplication::exec();
}