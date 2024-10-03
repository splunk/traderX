import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { TradeTicket } from 'main/app/model/trade.model';
import { Stock } from 'main/app/model/symbol.model';
import { Account } from 'main/app/model/account.model';
import { TypeaheadMatch } from 'ngx-bootstrap/typeahead';
import { trace, context, SpanKind, SpanStatusCode } from '@opentelemetry/api'; // Import OpenTelemetry API

@Component({
  selector: 'app-trade-ticket',
  templateUrl: './trade-ticket.component.html',
  styleUrls: ['./trade-ticket.component.scss']
})
export class TradeTicketComponent implements OnInit {

  @Input() stocks: Stock[];
  @Input() account: Account | undefined;

  @Output() create = new EventEmitter<TradeTicket>();
  @Output() cancel = new EventEmitter();

  selectedCompany?: string = undefined;
  ticket: TradeTicket;
  filteredStocks: Stock[] = [];

  ngOnInit() {
    this.ticket = {
      quantity: 0,
      accountId: this.account?.id || 0,
      side: 'Buy',
      security: ''
    };

    this.filteredStocks = this.stocks;
  }

  onSelect(e: TypeaheadMatch): void {
    console.log('Selected value: ', e.value);
    this.ticket.security = e.item.ticker;
  }

  onBlur(): void {
    if (this.selectedCompany) return;
    this.ticket.security = '';
  }

  onCreate() {
    if (!this.ticket.security || !this.ticket.quantity) {
      console.warn('Either security is not selected or quantity is not set!');
      return;
    }
  
    // OpenTelemetry: Start a span for the emit event
    const tracer = trace.getTracer('angular-trade-ticket'); // Use a meaningful tracer name
    const span = tracer.startSpan('socket.io.emit', {
      kind: SpanKind.PRODUCER,
      attributes: {
        'messaging.system': 'socket.io',
        'messaging.destination': 'trade-ticket',
        'messaging.operation': 'emit'
      }
    });
  
    try {
      // Bind the span to the context for proper propagation
      context.with(trace.setSpan(context.active(), span), () => {
        // Add traceParent to the ticket
        const activeSpan = trace.getSpan(context.active());

        const traceParent = activeSpan 
          ? `00-${activeSpan.spanContext().traceId}-${activeSpan.spanContext().spanId}-${activeSpan.spanContext().traceFlags.toString(16).padStart(2, '0')}`
          : "00-00000000000000000000000000000000-0000000000000000-00";

        // Clone the ticket and add the traceParent field
        const ticketWithTraceParent = {
          ...this.ticket,
          traceParent: traceParent // Adding traceParent to the ticket
        };

        // Emit the ticket creation event with traceParent
        console.log('create tradeTicket', ticketWithTraceParent);
        this.create.emit(ticketWithTraceParent);
      });
    } catch (error: any) {
      // Ensure error is an instance of Error
        if (error instanceof Error) {
          span.recordException(error);
          span.setStatus({ code: 2, message: error.message }); // 2 = ERROR
        } else {
          span.recordException({ message: 'Unknown error occurred' });
        }
      } finally {
      // End the span after the emit event completes, regardless of success or failure
      span.end();
    }
  }

  onCancel() {
    this.cancel.emit();
  }
}