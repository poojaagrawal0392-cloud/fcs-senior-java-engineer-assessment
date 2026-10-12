# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**
I would first agree on the cost model and the level at which leaders need decisions: shipment,
order, product, warehouse, store, or period. Cost pools should distinguish direct costs (labor
hours, storage, handling, carrier charges) from shared overhead (rent, utilities, systems), and
the allocation rules should be explainable, consistent, and auditable. A shared transportation
cost, for example, could be allocated by weight, volume, distance, or shipment count; the choice
changes the result and should reflect the actual cost driver rather than convenience.

I would gather source-system ownership, currency and tax treatment, time zones, posting
frequency, correction/credit handling, inventory valuation policy, and the expected reporting
latency. I would also ask whether costs need actual, standard, and landed-cost views; how
returns, split shipments, transfers, and shared labor are treated; and who approves changes to
allocation rules. I would validate the model against a sample of reconciled finance periods and
publish data-quality and reconciliation exceptions rather than silently allocating incomplete
costs.

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**
I would look for service-level and cost baselines before proposing savings: cost per order/line,
on-time delivery, fill rate, picking accuracy, utilization, inventory turns, overtime, and
expedited-shipping rates. Candidates include slotting and pick-path improvements, better labor
forecasting, carrier/service selection, shipment consolidation, packaging right-sizing, inventory
rebalancing, and reducing avoidable handling or returns. Each idea needs an operational owner,
estimated implementation cost, expected savings, and a service-risk assessment.

I would rank initiatives by net benefit, time-to-value, confidence in the data, reversibility,
and customer impact. Start with a baseline and a pilot/control comparison, agree stop/go
thresholds, and monitor both cost and service metrics after rollout. Savings should be measured
against a normalized baseline (volume, seasonality, and mix), not just a before/after total.
Where a change shifts costs between a warehouse, store, or carrier, evaluate end-to-end cost so
local optimization does not worsen total fulfillment economics.
[ fill here your answer ]

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**
Integration can provide finance with timely, traceable operational costs and let operations see
the financial impact of fulfillment decisions. Before choosing an interface I would identify the
systems of record for purchase orders, invoices, labor, inventory, and the chart of accounts,
along with data owners, identifiers, posting periods, and the required freshness and volume.
I would clarify whether "real time" means an event within seconds or a frequent, reconciled
batch, and how closed periods and late adjustments are handled.

I would define versioned contracts and stable business identifiers, then use reliable delivery
(idempotency keys, retries with backoff, dead-letter/error queues, and replay) rather than
assuming a network call always succeeds. Validate schemas and accounting dimensions at ingress,
secure credentials and sensitive finance data, and maintain audit trails from source event to
ledger entry. Reconciliation totals, lag, duplicate/rejected records, and unresolved exceptions
should be monitored; an initial shadow/reconciliation period can prove parity before automated
posting is enabled.
[ fill here your answer ]

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**
Forecast quality depends on agreed cost drivers and trustworthy historical data. I would identify
the planning horizon, forecast granularity, refresh cadence, and decisions the forecast supports:
labor and shift plans, storage capacity, carrier procurement, or annual budgets. Inputs could
include expected orders and units, product/store mix, promotions, seasonality, labor rates,
carrier tariffs, fuel, exchange rates, capacity constraints, and known network changes.

The design should retain actuals and forecast versions, assumptions, scenarios, and approval
history. It should compare forecast to actual, expose the largest variances and drivers, and
support baseline, optimistic, and downside scenarios rather than presenting false precision.
I would back-test forecasts by horizon and segment, track bias and error, and allow controlled
business overrides with an owner and rationale. Forecasts should be refreshed when new demand or
rate information arrives and translated into operational capacity plans.
[ fill here your answer ]

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**
Replacement creates a new physical/operational asset while reusing a business-unit identifier.
The system therefore needs an immutable warehouse-instance identity (or effective-dated record)
in addition to the reusable business-unit code. Archive the former instance; do not overwrite
its cost history. Link the replacement to its predecessor and retain effective dates, location,
capacity, and reason for change so finance can report both by physical instance and by continuing
business unit.

Before moving stock or orders, I would reconcile inventory quantity and valuation, outstanding
purchase orders, in-transit goods, labor commitments, leases, utilities, and transition costs.
Set a cutover date and define which costs belong to the old versus new instance, including
one-time closure, move, and ramp-up costs. Compare the new site's budget and unit-cost targets
against the old operation, forecast initial inefficiency and capacity, and monitor actual spend
and service during stabilization. Finance and operations should approve the reconciliation and
cutover, while reports preserve the full historical audit trail.
[ fill here your answer ]

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.

Across all scenarios, I would first establish decision owners, intended users, measurable
business outcomes, the baseline period, geography and operating units in scope, and the
available data sources and their quality. I would confirm regulatory/accounting constraints,
security and retention requirements, integration owners, expected scale and latency, and
dependencies on operational processes. I would then agree on an incremental delivery boundary,
success measures (cost and service), reconciliation/acceptance criteria, and the people
responsible for ongoing data and model governance.
