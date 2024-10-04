import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { AppModule } from './app/app.module';
import { environment } from './environments/environment';

/* import { SplunkRum } from '@splunk/otel-web';

SplunkRum.init({
    beaconEndpoint: 'https://ingest.eu0.signalfx.com/v1/traces',
    rumAuth: 'xxxxxx',
    app: 'Splunk-trading-app',
    environment: 'traderx-workshop',
    globalAttributes: {
      environment: 'traderx-workshop'
    }
  }); */


if (environment.production) {
    enableProdMode();
}

platformBrowserDynamic()
    .bootstrapModule(AppModule)
    .catch((err) => console.error(err));
