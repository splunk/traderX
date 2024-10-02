import { Injectable } from '@angular/core';
import { environment } from 'main/environments/environment';
import { io, Socket } from "socket.io-client";
import { trace, context, Span } from '@opentelemetry/api';

@Injectable({
    providedIn: 'root'
})
export class TradeFeedService {
    private socket: Socket;

    constructor() {
        this.connect();
    }

    private connect() {
        // create socketio client with long polling only
        this.socket = io(environment.tradeFeedUrl);
        
        this.socket.on("connect", this.onConnect);
        this.socket.on("disconnect", this.onDisconnect);
    }

    private onConnect = () => {
        console.log('Trade feed is connected, connection id ' + this.socket.id);
    }

    private onDisconnect = () => {
        console.log('Trade feed is disconnected, connection id was ' + this.socket.id);
    }

    public subscribe(topic: string, callback: (...args: any[]) => void) {
        const callbackFn = (args: any) => {
            console.log("Received message -> " + JSON.stringify(args));

            // Check if the message contains the traceParent
            if (args.traceParent) {
                console.log("Extracted traceParent: " + args.traceParent);

                // Remove traceParent from the message
                delete args.traceParent;
                console.log("traceParent removed from message.");
            }

            if (args.from !== 'System' && args.topic === topic) {
                callback(args.payload); // Pass the payload without the traceParent
            }
        };

        this.socket.on('publish', callbackFn);
        this.socket.emit('subscribe', topic);
        console.log('Subscribing to topic: ' + topic);

        return () => {
            this.unSubscribe(topic, callbackFn);
        };
    }

    public unSubscribe(topic: string, callback: (...args: any[]) => void) {
        console.log('Unsubscribing from topic: ' + topic);
        this.socket.emit('unsubscribe', topic);
        this.socket.off('publish', callback);
    }
}