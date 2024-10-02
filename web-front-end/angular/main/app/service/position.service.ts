/* import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, retry, tap } from 'rxjs/operators';
import { Trade, Position } from '../model/trade.model';
import { environment } from 'main/environments/environment';

@Injectable({
    providedIn: 'root'
})
export class PositionService {
    private tradesUrl = `${environment.positionsUrl}/trades/`;
    private positionsUrl = `${environment.positionsUrl}/positions/`;
    constructor(private http: HttpClient) { }

    getTrades(account_id: number): Observable<Trade[]> {
        return this.http.get<Trade[]>(this.tradesUrl + account_id ).pipe(
            catchError(this.handleError)
        );
    }

    getPositions(account_id: number): Observable<Position[]> {
        return this.http.get<Position[]>(this.positionsUrl + account_id ).pipe(
            catchError(this.handleError)
        );
    }

    private handleError(error: HttpErrorResponse) {
        console.error(error);
        return throwError(() => error);
    }
} */

import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Trade, Position } from '../model/trade.model';
import { environment } from 'main/environments/environment';

@Injectable({
    providedIn: 'root'
})
export class PositionService {
    private tradesUrl = `${environment.positionsUrl}/trades/`;
    private positionsUrl = `${environment.positionsUrl}/positions/`;

    constructor(private http: HttpClient) { }

    getTrades(account_id: number): Observable<Trade[]> {
        return this.http.get<Trade[]>(this.tradesUrl + account_id).pipe(
            tap(response => this.extractAndRemoveTraceParent(response)),  // Extract and remove traceParent
            catchError(this.handleError)
        );
    }

    getPositions(account_id: number): Observable<Position[]> {
        return this.http.get<Position[]>(this.positionsUrl + account_id).pipe(
            tap(response => this.extractAndRemoveTraceParent(response)),  // Extract and remove traceParent
            catchError(this.handleError)
        );
    }

    // Method to extract and remove traceParent
    private extractAndRemoveTraceParent(response: any): void {
        if (response && response.traceParent) {
            const traceParent = response.traceParent;
            console.log(`Extracted traceParent: ${traceParent}`);
            
            // Remove the traceParent from the response
            delete response.traceParent;
            console.log('traceParent removed from response');
        } else {
            console.warn('No traceParent found in the response.');
        }
    }

    private handleError(error: HttpErrorResponse) {
        console.error(error);
        return throwError(() => error);
    }
}
