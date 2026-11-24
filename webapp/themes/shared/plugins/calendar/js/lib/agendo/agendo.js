/**
 * Agendo — zero-dependency day/week/month, resource and timeline views
 * with drag to create, move and resize.
 *
 * Forked from simple-week-calendar-js by Mark McLaren
 * (https://github.com/markmclaren/simple-week-calendar-js), itself based on
 * jquery-week-calendar by Rob Monie (https://github.com/themouette/jquery-week-calendar).
 * MIT License, see LICENSE.
 */

const minutesOf = d => d.getHours() * 60 + d.getMinutes();
const atMinutes = (day, min) => { const d = new Date(day); d.setHours(0, min, 0, 0); return d; };
const addDays = (day, n) => { const d = new Date(day); d.setDate(d.getDate() + n); return d; };
const sameDay = (a, b) => a.toDateString() === b.toDateString();
const clamp = (v, lo, hi) => Math.min(hi, Math.max(lo, v));
const esc = s => String(s).replace(/[&<>"']/g, c => `&#${c.charCodeAt(0)};`);
const div = (className, text = '') => Object.assign(document.createElement('div'), { className, textContent: text });
const WD = ['SU', 'MO', 'TU', 'WE', 'TH', 'FR', 'SA'];   // iCalendar weekday codes, indexed by getDay()
const pad2 = n => String(n).padStart(2, '0');
const dayKey = d => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
const localDT = d => `${dayKey(d)}T${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
const parseDay = s => { const [y, m, d] = String(s).split('-').map(Number); return new Date(y, m - 1, d); };
// 'YYYY-MM-DD' is a local date (new Date() would read it as UTC midnight).
const toDate = v => (typeof v === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(v) ? parseDay(v) : new Date(v));
const dayDiff = (a, b) => Math.round((atMinutes(b, 0) - atMinutes(a, 0)) / 864e5);
// Background events: shaded areas behind the other events (display: 'background' | 'inverse-background').
const isBg = ev => ev.display === 'background' || ev.display === 'inverse-background';
// Parts of [lo, hi) that no [start, end] span covers.
const gaps = (spans, lo, hi) => {
  const out = [];
  let at = lo;
  for (const [s, e] of [...spans].sort((a, b) => a[0] - b[0])) {
    if (s > at) out.push([at, Math.min(s, hi)]);
    at = Math.max(at, e);
  }
  if (at < hi) out.push([at, hi]);
  return out.filter(([s, e]) => e > s);
};

class Agendo {
  static defaults = {
    date: new Date(),
    daysToShow: 7,          // 7 = week, 5 = workweek, 1 = day, 'month' = month grid,
                            // 'resource-day' / 'resource-workweek' / 'resource-week' = one column per resource,
                            // 'timeline-day' / 'timeline-week' / 'timeline-month' / 'timeline-custom'
                            //   = one row per resource, time running left to right,
                            // 'list-day' / 'list-week' / 'list-month' = events listed by day,
                            // 'year' = twelve mini months
    views: null,            // view buttons to show, e.g. [1, 7, 'month', 'list-week'] or with whole menus
                            // ['day', 'week', 'list', 'timeline']; null = all of them
    resources: [],          // [{ id, title, color? }] — events link to one through `resourceId`
    calendars: [],          // [{ id, title, color?, visible? }] — events link to one through `calendarId`
    categories: [],         // [{ id, title, color?, visible? }] — events link to one through `categoryId`
                            // both feed the Filter menu; visible: false starts the item unchecked
    firstDayOfWeek: 1,      // 0 = Sunday, 1 = Monday
    weekNumbers: false,     // week numbers in every view (grid corner, month / year column, list, timeline)
    weekNumberCalculation: 'auto',  // 'ISO' (ISO 8601) | 'local' (week 1 contains 1 January, weeks start on
                            // firstDayOfWeek) | 'auto' (ISO when weeks start on Monday) | (date) => number
    startHour: 0,
    endHour: 24,
    slotsPerHour: 4,        // 4 = 15-minute slots
    slotHeight: 24,         // px
    use24Hour: false,
    locale: navigator.language,
    businessHours: { start: 9, end: 18 },
    events: [],
    readonly: false,
    enableCreate: true,
    enableMove: true,
    enableResize: true,
    enableAllDay: true,     // "All day" in the event form, and drag to create in the all-day row
    enableRecurring: true,  // "Repeat" in the event form (existing recurring events still show)
    showCurrentTime: true,
    allDaySlot: true,       // all-day row under the day headers of the day / week / resource views
    monthMaxEvents: 3,      // events listed per day cell in month view before "+N more"
    timelineCustom: null,   // { days: 3, slotMinutes: 30, labelMinutes: 60, slotWidth: 24, label: '3 days' }
                            // enables 'timeline-custom'
    eventPopover: true,     // card with the event's details on mouse hover; false = none,
                            // or (event) => Node | string | null for your own content
    resourceLabel: 'Resources',  // header of the resource column in timeline views
    colorScheme: null,      // 'light' | 'dark' | 'auto' (follows the OS) | null (light, or the page's framework theme)
    theme: null,            // e.g. 'tabler' | 'bootstrap' | 'bulma': adds class ago-theme-<name> (load themes/<name>.css)
    onEventClick: null,     // (event) => {}  — defaults to opening the edit dialog
    onEventChange: null,    // (event, 'move' | 'resize' | 'edit' | 'exclude' | 'truncate', info?) => {}
    onEventCreate: null,    // (event, info?) => {}
    onEventDelete: null,    // (id, info?) => {}
                            // info = { scope: 'this' | 'following' | 'all', recurringId, occurrence }
                            // when the change comes from an occurrence of a recurring event
    onFilterChange: null    // ({ calendars, categories }) => {} — ids still shown
  };

  constructor(container, options = {}) {
    this.el = typeof container === 'string' ? document.querySelector(container) : container;
    if (!this.el) throw new Error('Agendo: container not found');

    const o = this.o = { ...Agendo.defaults, ...options };
    this.date = new Date(o.date);
    this.slot = 60 / o.slotsPerHour;
    this.startMin = o.startHour * 60;
    this.endMin = o.endHour * 60;

    const hour12 = !o.use24Hour;
    this.fmt = {
      weekday: new Intl.DateTimeFormat(o.locale, { weekday: 'short' }),
      hour: new Intl.DateTimeFormat(o.locale, { hour: 'numeric', hour12 }),
      time: new Intl.DateTimeFormat(o.locale, { hour: 'numeric', minute: '2-digit', hour12 }),
      range: new Intl.DateTimeFormat(o.locale, { day: 'numeric', month: 'long', year: 'numeric' }),
      dayShort: new Intl.DateTimeFormat(o.locale, { weekday: 'short', day: 'numeric', month: 'short' }),
      weekdayLong: new Intl.DateTimeFormat(o.locale, { weekday: 'long' }),
      weekdayNarrow: new Intl.DateTimeFormat(o.locale, { weekday: 'narrow' }),
      monthLong: new Intl.DateTimeFormat(o.locale, { month: 'long' }),
      year: new Intl.DateTimeFormat(o.locale, { year: 'numeric' }),
      dayLong: new Intl.DateTimeFormat(o.locale, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }),
      dayMonth: new Intl.DateTimeFormat(o.locale, { day: 'numeric', month: 'long' }),
      month: new Intl.DateTimeFormat(o.locale, { month: 'long', year: 'numeric' })
    };

    this._build();
    this._applyResources(o.resources);
    this._applyFilterLists(o.calendars, o.categories);
    if (!this._viewAllowed(o.daysToShow)) o.daysToShow = this._firstView();
    this.setEvents(o.events);
    this.scrollToHour(this._defaultHour());
    if (o.showCurrentTime) this.timer = setInterval(() => this._renderNow(), 60_000);
  }

  // --- Structure (built once) ---

  _build() {
    const o = this.o, hourPx = o.slotHeight * o.slotsPerHour, biz = o.businessHours;
    this.el.classList.add('agendo');
    this.setTheme(o.theme);
    this.setColorScheme(o.colorScheme);
    Object.entries({
      '--ago-slot-h': `${o.slotHeight}px`,
      '--ago-hour-h': `${hourPx}px`,
      '--ago-hours': o.endHour - o.startHour,
      '--ago-biz-top': `${biz ? (biz.start - o.startHour) * hourPx : 0}px`,
      '--ago-biz-bottom': `${((biz ? biz.end : o.endHour) - o.startHour) * hourPx}px`
    }).forEach(([k, v]) => this.el.style.setProperty(k, v));

    // Weekday toggles of the custom repeat, in the calendar's week order.
    const sunday = new Date(2023, 0, 1);
    const weekdays = Array.from({ length: 7 }, (_, i) => {
      const wd = (o.firstDayOfWeek + i) % 7, d = addDays(sunday, wd);
      return `<label title="${esc(this.fmt.weekdayLong.format(d))}"><input type="checkbox" name="byDay" value="${WD[wd]}">` +
        `<span>${esc(this.fmt.weekdayNarrow.format(d))}</span></label>`;
    }).join('');

    this.el.innerHTML = `
      <div class="ago-toolbar">
        <div class="ago-group">
          <button type="button" class="ago-btn" data-action="today">Today</button>
          <button type="button" class="ago-btn" data-action="prev" title="Previous">&larr;</button>
          <button type="button" class="ago-btn" data-action="next" title="Next">&rarr;</button>
        </div>
        <div class="ago-title"></div>
        <div class="ago-tools">
        <div class="ago-views">
          <div class="ago-group">
            <button type="button" class="ago-btn" data-days="1">Day</button>
            <button type="button" class="ago-btn" data-days="5">Workweek</button>
            <button type="button" class="ago-btn" data-days="7">Week</button>
            <button type="button" class="ago-btn" data-days="month">Month</button>
            <button type="button" class="ago-btn" data-days="year">Year</button>
            <details class="ago-dropdown">
              <summary class="ago-btn" data-label="List">List</summary>
              <div class="ago-menu">
                <button type="button" data-days="list-day">Day</button>
                <button type="button" data-days="list-week">Week</button>
                <button type="button" data-days="list-month">Month</button>
              </div>
            </details>
          </div>
          <div class="ago-group">
            <details class="ago-dropdown">
              <summary class="ago-btn" data-label="Resources">Resources</summary>
              <div class="ago-menu">
                <button type="button" data-days="resource-day">Day</button>
                <button type="button" data-days="resource-workweek">Workweek</button>
                <button type="button" data-days="resource-week">Week</button>
              </div>
            </details>
            <details class="ago-dropdown">
              <summary class="ago-btn" data-label="Timeline">Timeline</summary>
              <div class="ago-menu">
                <button type="button" data-days="timeline-day">Day</button>
                <button type="button" data-days="timeline-week">Week</button>
                <button type="button" data-days="timeline-month">Month</button>
                <button type="button" data-days="timeline-custom"></button>
              </div>
            </details>
          </div>
        </div>
        <details class="ago-dropdown ago-filter">
          <summary class="ago-btn">Filter</summary>
          <div class="ago-menu"></div>
        </details>
        </div>
      </div>
      <div class="ago-grid"></div>
      <dialog class="ago-dialog">
        <form method="dialog">
          <header><span class="ago-dialog-title"></span><button value="cancel" formnovalidate>&times;</button></header>
          <label>Title <input name="title" required></label>
          <label class="ago-check" data-allday-field><input type="checkbox" name="allDay"> All day</label>
          <label>Start <input name="start" type="datetime-local" required></label>
          <label>End <input name="end" type="datetime-local" required></label>
          <div class="ago-repeat" data-repeat>
            <label>Repeat
              <select name="repeat">
                <option value="none"></option><option value="daily"></option><option value="weekly"></option>
                <option value="weekdays"></option><option value="monthly"></option><option value="monthlyNth"></option>
                <option value="yearly"></option><option value="custom"></option>
              </select>
            </label>
            <fieldset data-custom>
              <legend>Repeat every</legend>
              <div class="ago-inline">
                <input name="interval" type="number" min="1" max="99" value="1" aria-label="Interval">
                <select name="freq" aria-label="Unit">
                  <option value="daily">day(s)</option><option value="weekly">week(s)</option>
                  <option value="monthly">month(s)</option><option value="yearly">year(s)</option>
                </select>
              </div>
              <div class="ago-weekdays" data-weekdays role="group" aria-label="On">${weekdays}</div>
            </fieldset>
            <fieldset data-ends>
              <legend>Ends</legend>
              <label class="ago-check"><input type="radio" name="ends" value="never" checked> Never</label>
              <label class="ago-check"><input type="radio" name="ends" value="until"> On
                <input type="date" name="until" aria-label="End date"></label>
              <label class="ago-check"><input type="radio" name="ends" value="count"> After
                <input type="number" name="count" min="1" max="999" value="10" aria-label="Occurrences"> occurrences</label>
            </fieldset>
          </div>
          <label data-res-field>Resource <select name="resourceId"></select></label>
          <label data-cal-field>Calendar <select name="calendarId"></select></label>
          <label data-cat-field>Category <select name="categoryId"></select></label>
          <footer>
            <button class="ago-btn ago-btn-danger" value="delete" formnovalidate>Delete</button>
            <button class="ago-btn" value="cancel" formnovalidate>Cancel</button>
            <button class="ago-btn ago-btn-primary" value="save">Save</button>
          </footer>
        </form>
      </dialog>
      <dialog class="ago-dialog ago-scope">
        <form method="dialog">
          <header><span class="ago-dialog-title"></span></header>
          <div class="ago-scope-list">
            <label class="ago-check" data-scope-this><input type="radio" name="scope" value="this" checked> This event</label>
            <label class="ago-check"><input type="radio" name="scope" value="following"> This and following events</label>
            <label class="ago-check"><input type="radio" name="scope" value="all"> All events</label>
          </div>
          <footer>
            <button class="ago-btn" value="cancel" formnovalidate>Cancel</button>
            <button class="ago-btn ago-btn-primary" value="ok">OK</button>
          </footer>
        </form>
      </dialog>
      <div class="ago-pop" popover="manual" role="tooltip"></div>`;

    const $ = sel => this.el.querySelector(sel);
    this.titleEl = $('.ago-title');
    this.grid = $('.ago-grid');
    this.dialog = $('dialog');
    this.form = $('form');
    this.scopeDialog = $('.ago-scope');
    this._bindPopover();

    let gutter = '';
    for (let h = o.startHour; h < o.endHour; h++) {
      gutter += `<div><span>${this.fmt.hour.format(atMinutes(new Date(), h * 60))}</span></div>`;
    }
    this.gutterHtml = `<div class="ago-gutter">${gutter}</div>`;

    $('.ago-toolbar').addEventListener('click', e => {
      const btn = e.target.closest('button');
      const v = btn?.dataset.days;
      if (v) {
        this._closeMenus();
        this.setView(isNaN(v) ? v : +v);
      }
      else if (btn) this[btn.dataset.action]();
    });

    // Filter menu: each checkbox shows or hides the events of one calendar or category.
    $('.ago-filter .ago-menu').addEventListener('change', e => {
      const box = e.target, kind = box.closest('[data-kind]')?.dataset.kind;
      if (!kind) return;
      this.hidden[kind][box.checked ? 'delete' : 'add'](box.value);
      this._filterChanged();
    });

    // Dropdowns: only one open at a time; close on outside click or Escape.
    const menus = [...this.el.querySelectorAll('.ago-dropdown')];
    menus.forEach(m => m.addEventListener('toggle', () => m.open && menus.forEach(o => o !== m && (o.open = false))));
    this._onDocPointer = e => { if (!e.target.closest?.('.ago-dropdown')) this._closeMenus(); };
    document.addEventListener('pointerdown', this._onDocPointer);
    $('.ago-toolbar').addEventListener('keydown', e => {
      const open = e.key === 'Escape' && e.target.closest('.ago-dropdown[open]');
      if (open) { open.open = false; open.querySelector('summary').focus(); }
    });

    this.grid.addEventListener('pointerdown', e => this._onDown(e));
    this.grid.addEventListener('pointermove', e => this._onMove(e));
    this.grid.addEventListener('pointerup', e => this._onUp(e));
    this.grid.addEventListener('pointercancel', e => this._onUp(e));
    // Month and list views: a day number, "+N more" or a list date opens that day in day view.
    // List view: an event row opens it like a click on the event in the other views.
    this.grid.addEventListener('click', e => {
      const goto = e.target.closest('[data-goto]');
      if (goto) { this.o.daysToShow = 1; this.gotoDate(this.dates[+goto.dataset.goto]); return; }
      const week = e.target.closest('[data-week]');
      if (week) { this.o.daysToShow = 7; this.gotoDate(this.dates[+week.dataset.week]); return; }
      const month = e.target.closest('[data-month]');
      if (month) { this.o.daysToShow = 'month'; this.gotoDate(new Date(this.date.getFullYear(), +month.dataset.month, 1)); return; }
      const item = e.target.closest('.ago-list-item');
      const ev = item && !this.o.readonly && this._findShown(item.dataset.id);
      if (ev) this.o.onEventClick ? this.o.onEventClick(ev) : this.openEditModal(ev);
    });

    const f = this.form.elements;
    this._checkRange = () => {
      const { start: s, end: e } = this._readDates();
      f.end.setCustomValidity(e > s ? '' : 'End must be after start');
    };
    f.start.addEventListener('input', () => { this._checkRange(); this._repeatLabels(); });
    f.end.addEventListener('input', this._checkRange);
    f.allDay.addEventListener('change', () => this._toggleAllDay());
    $('[data-repeat]').addEventListener('change', e => {
      this._ruleTouched = true;
      // Custom starts from the preset that was chosen.
      if (e.target === f.repeat && f.repeat.value === 'custom') {
        const p = this._presetRule(this._prevRepeat, this._readDates().start);
        if (p) this._fillCustom(p, this._readDates().start);
      }
      if (e.target === f.repeat) this._prevRepeat = f.repeat.value;
      this._repeatUI();
    });
    this.form.addEventListener('submit', e => this._onSubmit(e));
  }

  // --- Rendering ---

  get _isMonth() { return this.o.daysToShow === 'month'; }

  render() {
    this.tl = this._tl();
    this.list = this._listRange();
    const kind = this._isMonth ? 'month' : this.o.daysToShow === 'year' ? 'year'
      : this.tl ? 'timeline' : this.list ? 'list' : 'grid';
    const viewChanged = this._shownView !== this.o.daysToShow, prevKind = this._kind;
    this._shownView = this.o.daysToShow;
    this._kind = kind;
    this.grid.classList.toggle('ago-month', kind === 'month');
    this.grid.classList.toggle('ago-tl', kind === 'timeline');
    this.grid.classList.toggle('ago-list', kind === 'list');
    this.grid.classList.toggle('ago-yearview', kind === 'year');
    this.el.querySelectorAll('[data-days]').forEach(b =>
      b.classList.toggle('active', b.dataset.days === String(this.o.daysToShow)));
    // A dropdown is highlighted and names the chosen view when it holds the active one.
    this.el.querySelectorAll('.ago-views .ago-dropdown').forEach(m => {
      const item = m.querySelector('[data-days].active'), sum = m.querySelector('summary');
      sum.classList.toggle('active', !!item);
      sum.textContent = item ? `${sum.dataset.label}: ${item.textContent}` : sum.dataset.label;
    });

    if (kind === 'month') this._renderMonth();
    else if (kind === 'timeline') this._renderResourceTimeline();
    else if (kind === 'list') this._renderList();
    else if (kind === 'year') this._renderYear();
    else this._renderTimeGrid();
    this._renderEvents();
    this._renderNow();

    if (kind === 'grid' && prevKind && prevKind !== 'grid') this.scrollToHour(this._defaultHour());
    if ((kind === 'list' || kind === 'year') && viewChanged) this.grid.scrollTop = 0;
    if (kind === 'timeline' && viewChanged) {
      this.grid.scrollTop = 0;
      const row = this.rows[0];
      this.grid.scrollLeft = row && !this.tl.month
        ? this._tlX(atMinutes(this.tl.first, this._defaultHour() * 60)) * row.offsetWidth : 0;
    }
  }

  // 'resource-day' → 1 day, 'resource-workweek' → 5 days, 'resource-week' → 7 days,
  // each split into one column per resource.
  _viewInfo() {
    const v = this.o.daysToShow;
    if (v === 'resource-day') return { days: 1, byRes: true };
    if (v === 'resource-workweek') return { days: 5, byRes: true };
    if (v === 'resource-week') return { days: 7, byRes: true };
    return { days: v, byRes: false };
  }

  // Time grid: one column per date, or per (date, resource) pair in resource views.
  // this.dates[i] / this.colRes[i] give the date and resource (or null) of column i.
  _renderTimeGrid() {
    const { days: n, byRes } = this._viewInfo();
    const first = n === 1 ? atMinutes(this.date, 0) : this._weekStart(this.date);
    const days = Array.from({ length: n }, (_, i) => addDays(first, i));
    const res = byRes && this.resources.length ? this.resources : [null];
    const R = res.length, grouped = R > 1 || res[0] !== null;
    this.dates = days.flatMap(d => res.map(() => d));
    this.colRes = days.flatMap(() => res);
    this.titleEl.textContent = n === 1
      ? this.fmt.range.format(days[0])
      : this.fmt.range.formatRange(days[0], days[n - 1]);

    const now = new Date();
    const sep = i => (grouped && n > 1 && i > 0 && i % R === 0 ? ' ago-date-start' : '');
    const dayHead = (d, i) => `
      <div class="ago-day-head${sameDay(d, now) ? ' ago-today' : ''}${sep(i * R)}" style="grid-column: span ${R}">
        <div class="ago-day-name">${this.fmt.weekday.format(d)}</div>
        <div class="ago-day-num">${d.getDate()}</div>
      </div>`;
    const resHead = (r, i) => `
      <div class="ago-res-head${sep(i)}" title="${esc(r.title)}">
        ${r.color ? `<span class="ago-res-dot" style="background:${esc(r.color)}"></span>` : ''}${esc(r.title)}
      </div>`;

    const wk = this.o.weekNumbers ? this._weekLabel(days[0]) : '';
    let head;
    if (!grouped) head = `<div class="ago-corner">${wk}</div>` + days.map(dayHead).join('');
    else if (n === 1) head = `<div class="ago-corner">${wk}</div>` + res.map(resHead).join('');
    else head = `<div class="ago-corner" style="grid-row: span 2">${wk}</div>` + days.map(dayHead).join('') +
      this.colRes.map(resHead).join('');   // dates above resources

    if (this.o.allDaySlot) head += '<div class="ago-allday-label">all-day</div><div class="ago-allday"></div>';

    this.grid.style.setProperty('--ago-days', this.dates.length);
    this.grid.style.setProperty('--ago-col-min', grouped && n > 1 ? '70px' : '0px');
    this.grid.innerHTML =
      `<div class="ago-head">${head}</div>` +
      this.gutterHtml +
      this.dates.map((_, i) => `<div class="ago-col${sep(i)}" data-day="${i}"><div class="ago-layer"></div></div>`).join('');

    this.cols = [...this.grid.querySelectorAll('.ago-col')];
    this.adRow = this.grid.querySelector('.ago-allday');
  }

  // --- Resource timeline: one row per resource, time running left to right ---

  // Geometry of the current timeline view, or null when the view is not a timeline.
  // Each day shows [startMin, endMin) cut into narrow slots of `slotMinutes`, labelled every
  // `labelMinutes`; `step` is the drag snap in minutes.
  _tl() {
    const hours = { startMin: this.startMin, endMin: this.endMin, step: this.slot };
    const wholeDays = { startMin: 0, endMin: 1440, slotMinutes: 1440, labelMinutes: 1440, step: 1440 };
    switch (this.o.daysToShow) {
      case 'timeline-day':
        return { ...hours, slotMinutes: 30, labelMinutes: 60, slotWidth: 32, days: 1, jump: 1, first: atMinutes(this.date, 0) };
      case 'timeline-week':
        return { ...hours, slotMinutes: 30, labelMinutes: 120, slotWidth: 16, days: 7, jump: 7, first: this._weekStart(this.date) };
      case 'timeline-month': {
        const first = new Date(this.date.getFullYear(), this.date.getMonth(), 1);
        const days = new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate();
        return { ...wholeDays, days, first, slotWidth: 40, month: true };
      }
      case 'timeline-custom': {
        const c = { days: 3, slotMinutes: 30, labelMinutes: 60, slotWidth: 24, ...this.o.timelineCustom };
        const base = c.slotMinutes >= 1440 ? wholeDays
          : { ...hours, slotMinutes: c.slotMinutes, labelMinutes: Math.max(c.labelMinutes, c.slotMinutes) };
        return { ...base, days: c.days, jump: c.days, first: atMinutes(this.date, 0), slotWidth: c.slotWidth };
      }
    }
    return null;
  }

  _renderResourceTimeline() {
    const t = this.tl;
    t.span = t.endMin - t.startMin;
    t.perDay = Math.ceil(t.span / t.slotMinutes);
    t.perLabel = Math.max(1, Math.round(t.labelMinutes / t.slotMinutes));
    const days = Array.from({ length: t.days }, (_, i) => addDays(t.first, i));
    const slots = t.days * t.perDay, now = new Date();
    this.dates = days;
    this.cols = [];
    this.colRes = [];
    this.titleEl.textContent = t.month ? this.fmt.month.format(t.first)
      : t.days === 1 ? this.fmt.range.format(t.first)
      : this.fmt.range.formatRange(t.first, days.at(-1));

    const today = d => (sameDay(d, now) ? ' ago-today' : '');
    const slotFmt = t.labelMinutes % 60 ? this.fmt.time : this.fmt.hour;
    // Week number at the first day and at each week start.
    const wk = (d, i) => (this.o.weekNumbers && (i === 0 || d.getDay() === this.o.firstDayOfWeek) ? this._weekLabel(d) : '');
    let head = '';
    if (t.slotMinutes >= 1440) {
      head = days.map((d, i) => `
        <div class="ago-tl-slot ago-tl-daycell${today(d)}">
          ${wk(d, i)}<div class="ago-day-name">${this.fmt.weekday.format(d)}</div><div>${d.getDate()}</div>
        </div>`).join('');
    } else {
      // Row 1: one centred date per day (pinned beside the resource column while scrolling).
      if (t.days > 1) {
        head = days.map((d, i) => `
          <div class="ago-tl-date${today(d)}" style="grid-column: span ${t.perDay}">
            <span class="ago-tl-pin">${this.fmt.dayShort.format(d)}${wk(d, i)}</span>
          </div>`).join('');
      }
      // Row 2: one centred time label every `perLabel` slots.
      head += days.map(d => {
        let out = '';
        for (let j = 0; j < t.perDay; j += t.perLabel) {
          out += `<div class="ago-tl-slot${j === 0 ? ' ago-tl-daystart' : ''}" style="grid-column: span ${Math.min(t.perLabel, t.perDay - j)}">` +
            `${slotFmt.format(atMinutes(d, t.startMin + j * t.slotMinutes))}</div>`;
        }
        return out;
      }).join('');
    }

    Object.entries({
      '--ago-tl-slots': slots,
      '--ago-tl-per-day': t.slotMinutes >= 1440 ? slots : t.perDay,
      '--ago-tl-per-label': t.perLabel,
      '--ago-tl-slot-w': `${t.slotWidth}px`
    }).forEach(([k, v]) => this.grid.style.setProperty(k, v));

    this.grid.innerHTML =
      `<div class="ago-tl-corner">${t.days === 1 && this.o.weekNumbers ? this._weekLabel(t.first) : ''}<span>${esc(this.o.resourceLabel)}</span></div><div class="ago-tl-head">${head}</div>` +
      this.resources.map((r, i) => `
        <div class="ago-tl-res" title="${esc(r.title)}">
          ${r.color ? `<span class="ago-res-dot" style="background:${esc(r.color)}"></span>` : ''}${esc(r.title)}
        </div>
        <div class="ago-tl-row" data-row="${i}"></div>`).join('');
    this.rows = [...this.grid.querySelectorAll('.ago-tl-row')];
  }

  // Date → horizontal position in [0, 1]. Hours outside [startMin, endMin) collapse onto the edges.
  _tlX(date) {
    const t = this.tl, k = Math.round((atMinutes(date, 0) - t.first) / 864e5);
    if (k < 0) return 0;
    if (k >= t.days) return 1;
    return (k * t.span + clamp(minutesOf(date), t.startMin, t.endMin) - t.startMin) / (t.days * t.span);
  }

  // Horizontal position in [0, 1] → date, snapped to `step` minutes.
  _tlDate(frac, round = Math.round) {
    const t = this.tl, total = clamp(frac, 0, 1) * t.days * t.span;
    const k = Math.min(t.days - 1, Math.floor(total / t.span));
    return atMinutes(addDays(t.first, k), t.startMin + round((total - k * t.span) / t.step) * t.step);
  }

  _tlAfter(date, n = 1) { return atMinutes(date, minutesOf(date) + n * this.tl.step); }

  _renderTimelineEvents() {
    const t = this.tl, end = addDays(t.first, t.days);
    const handles = !this.o.readonly && this.o.enableResize;
    const preview = this.drag?.mode === 'create' && this.drag.event;
    const box = (el, s, e) => Object.assign(el.style, { left: `${s * 100}%`, width: `${(e - s) * 100}%` });

    // Today's column is tinted in every row.
    const k = Math.round((atMinutes(new Date(), 0) - t.first) / 864e5);
    const band = k >= 0 && k < t.days && [k / t.days, (k + 1) / t.days];

    this.rows.forEach((row, i) => {
      const r = this.resources[i];
      const items = this._shownEvents
        .filter(ev => !isBg(ev) && ev.resourceId === r.id && ev.start < end && ev.end > t.first)
        .map(ev => ({ ev, s: this._tlX(ev.start), e: this._tlX(ev.end) }))
        .sort((a, b) => a.s - b.s || b.e - a.e);

      // Overlapping events stack in lanes; the row grows to fit them.
      const laneEnds = [];
      for (const it of items) {
        let lane = laneEnds.findIndex(x => x <= it.s);
        if (lane < 0) lane = laneEnds.length;
        laneEnds[lane] = it.e;
        it.lane = lane;
      }
      row.style.setProperty('--ago-lanes', Math.max(1, laneEnds.length));

      const els = items.map(({ ev, s, e, lane }) => {
        const el = div('ago-event ago-tl-event' + (this.drag?.event === ev ? ' is-dragging' : '') +
          (ev.start < t.first ? ' ago-clip-start' : '') + (ev.end > end ? ' ago-clip-end' : ''));
        el.dataset.id = ev.id;
        box(el, s, e);
        el.style.top = `calc(${lane} * var(--ago-lane-h) + 3px)`;
        el.style.setProperty('--ago-ev', this._colorOf(ev));
        if (!this.o.eventPopover) el.title = ev.allDay ? `${ev.title} (all day)` : `${this.fmt.time.format(ev.start)} – ${this.fmt.time.format(ev.end)} ${ev.title}`;
        if (handles) {
          el.append(div('ago-handle'), div('ago-handle'));
          el.children[0].dataset.edge = 'start';
          el.children[1].dataset.edge = 'end';
        }
        const label = div('ago-tl-pin');
        if (!t.month && !ev.allDay) label.append(div('ago-event-time', this.fmt.time.format(ev.start)));
        label.append(this._title(ev, 'ago-event-title'));
        el.append(label);
        return el;
      });
      // Background events of this resource, or of every resource when they have none.
      const bgs = this._bgEvents.filter(ev => !ev.resourceId || ev.resourceId === r.id);
      const inRange = ev => ev.start < end && ev.end > t.first;
      const inv = bgs.filter(ev => ev.display === 'inverse-background');
      const bgEls = bgs.filter(ev => ev.display === 'background' && inRange(ev))
        .map(ev => this._bgEl(ev, el => box(el, this._tlX(ev.start), this._tlX(ev.end)), 'ago-tl-pin'));
      if (inv.length) {
        for (const [s, e] of gaps(inv.filter(inRange).map(ev => [this._tlX(ev.start), this._tlX(ev.end)]), 0, 1)) {
          bgEls.push(this._bgEl(inv[0], el => box(el, s, e)));
        }
      }
      els.unshift(...bgEls);
      if (band) {
        const b = div('ago-tl-today');
        box(b, ...band);
        els.unshift(b);
      }
      if (preview && preview.resourceId === r.id) {
        const p = div('ago-preview ago-tl-preview');
        box(p, this._tlX(preview.start), this._tlX(preview.end));
        els.push(p);
      }
      row.replaceChildren(...els);
    });
  }

  _onTlDown(e) {
    const row = e.target.closest('.ago-tl-row');
    if (this.o.readonly || e.button !== 0 || !row) return;
    const evEl = e.target.closest('.ago-event');
    const edge = e.target.closest('.ago-handle')?.dataset.edge;
    const event = evEl && this._findShown(evEl.dataset.id);
    const mode = evEl ? (edge || 'move') : 'create';
    if (mode === 'create' && !this.o.enableCreate) return;

    const rect = row.getBoundingClientRect();
    this.drag = {
      tl: true, mode, rect, row: +row.dataset.row, x: e.clientX, y: e.clientY, moved: false,
      anchor: this._tlDate((e.clientX - rect.left) / rect.width, Math.floor),
      x0: event && this._tlX(event.start),
      event: event ?? { start: null, end: null },
      orig: event && { start: event.start, end: event.end, resourceId: event.resourceId }
    };
    this.grid.setPointerCapture(e.pointerId);
    if (mode === 'create') this._onTlMove(e, true);
  }

  _onTlMove(e, force = false) {
    const d = this.drag, dx = e.clientX - d.x;
    if (!d.moved && !force && Math.hypot(dx, e.clientY - d.y) < 4) return;
    d.moved = !force || d.moved;

    const ev = d.event, frac = (e.clientX - d.rect.left) / d.rect.width;
    // All-day events move and resize by whole days.
    if (ev.allDay && d.mode !== 'create') {
      const dayAt = () => addDays(this.tl.first, clamp(Math.round(frac * this.tl.days), 0, this.tl.days));
      if (d.mode === 'move') {
        if (!this.o.enableMove) return;
        const shift = Math.round(dx / (d.rect.width / this.tl.days));
        ev.start = addDays(d.orig.start, shift);
        ev.end = addDays(d.orig.end, shift);
        const row = document.elementFromPoint(e.clientX, e.clientY)?.closest('.ago-tl-row');
        if (row && this.grid.contains(row)) ev.resourceId = this.resources[+row.dataset.row].id;
      } else if (d.mode === 'start') {
        const s = dayAt();
        ev.start = s < d.orig.end ? s : addDays(d.orig.end, -1);
      } else {
        const en = dayAt();
        ev.end = en > d.orig.start ? en : addDays(d.orig.start, 1);
      }
      return this._renderEvents();
    }
    switch (d.mode) {
      case 'create': {
        const cur = this._tlDate(frac, Math.floor);
        ev.start = cur < d.anchor ? cur : d.anchor;
        ev.end = this._tlAfter(cur < d.anchor ? d.anchor : cur);
        ev.resourceId = this.resources[d.row].id;
        break;
      }
      case 'move': {
        if (!this.o.enableMove) return;
        ev.start = this._tlDate(d.x0 + dx / d.rect.width);
        ev.end = new Date(+ev.start + (d.orig.end - d.orig.start));
        // vertical drag onto another row changes the resource
        const row = document.elementFromPoint(e.clientX, e.clientY)?.closest('.ago-tl-row');
        if (row && this.grid.contains(row)) ev.resourceId = this.resources[+row.dataset.row].id;
        break;
      }
      case 'start': {
        const s = this._tlDate(frac);
        ev.start = s < d.orig.end ? s : this._tlAfter(d.orig.end, -1);
        break;
      }
      case 'end': {
        const en = this._tlDate(frac);
        ev.end = en > d.orig.start ? en : this._tlAfter(d.orig.start);
        break;
      }
    }
    this._renderEvents();
  }

  // --- List: events grouped by day, one row each ---

  _listRange() {
    switch (this.o.daysToShow) {
      case 'list-day': return { first: atMinutes(this.date, 0), days: 1, jump: 1 };
      case 'list-week': return { first: this._weekStart(this.date), days: 7, jump: 7 };
      case 'list-month': {
        const first = new Date(this.date.getFullYear(), this.date.getMonth(), 1);
        return { first, days: new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate(), month: true };
      }
    }
    return null;
  }

  _renderList() {
    const l = this.list;
    this.dates = Array.from({ length: l.days }, (_, i) => addDays(l.first, i));
    this.cols = [];
    this.titleEl.textContent = l.month ? this.fmt.month.format(l.first)
      : l.days === 1 ? this.fmt.range.format(l.first)
      : this.fmt.range.formatRange(l.first, this.dates.at(-1));
  }

  _renderListEvents() {
    const now = new Date(), t = d => this.fmt.time.format(d);
    const parts = [];
    let lastWeek = null;
    this.dates.forEach((day, i) => {
      const next = addDays(day, 1);
      // An event is listed on every day it overlaps (zero-length events on their start day).
      const items = this._shownEvents
        .filter(ev => !isBg(ev) && ev.start < next && (ev.end > day || sameDay(ev.start, day)))
        .sort((a, b) => a.start - b.start || b.end - a.end);
      if (!items.length) return;

      const head = div('ago-list-head' + (sameDay(day, now) ? ' ago-today' : ''));
      const goto = Object.assign(document.createElement('button'), {
        type: 'button', className: 'ago-list-goto', textContent: this.fmt.weekdayLong.format(day)
      });
      goto.dataset.goto = i;
      const left = div('ago-list-day');
      left.append(goto);
      // Week number at the first listed day and at each new week.
      const week = this.o.weekNumbers && this._weekNumber(day);
      if (week && week !== lastWeek) left.insertAdjacentHTML('beforeend', this._weekLabel(day));
      lastWeek = week;
      head.append(left, Object.assign(document.createElement('span'), { textContent: this.fmt.range.format(day) }));
      parts.push(head);

      for (const ev of items) {
        // Like FullCalendar: "start – end" within the day, "start –" / "– end" on the first / last
        // day of a multi-day event, "all day" in between or for a whole-day event.
        const startsHere = ev.start >= day, endsHere = ev.end < next;
        const fromMidnight = +ev.start <= +day, toMidnight = +ev.end >= +next;
        const time = fromMidnight && toMidnight ? 'all day'
          : startsHere && endsHere ? (ev.end > ev.start ? `${t(ev.start)} – ${t(ev.end)}` : t(ev.start))
          : startsHere ? `${t(ev.start)} –`
          : `– ${t(ev.end)}`;
        const row = Object.assign(document.createElement('button'), { type: 'button', className: 'ago-list-item' });
        row.dataset.id = ev.id;
        row.style.setProperty('--ago-ev', this._colorOf(ev));
        const res = this.resources.find(r => r.id === ev.resourceId);
        row.append(div('ago-list-time', time), div('ago-list-dot'), this._title(ev, 'ago-list-title'));
        if (res) row.lastChild.append(Object.assign(document.createElement('span'), { className: 'ago-list-res', textContent: res.title }));
        parts.push(row);
      }
    });
    this.grid.replaceChildren(...(parts.length ? parts : [div('ago-list-empty', 'No events to display')]));
  }

  // --- Year: twelve mini months; days with events carry coloured dots ---

  _renderYear() {
    const y = this.date.getFullYear(), now = new Date(), fdw = this.o.firstDayOfWeek;
    this.titleEl.textContent = this.fmt.year.format(new Date(y, 0, 1));
    this.dates = [];
    this.cols = [];
    const week0 = this._weekStart(now);
    const wn = this.o.weekNumbers;
    const heads = (wn ? '<div class="ago-yhead"></div>' : '') + Array.from({ length: 7 }, (_, i) =>
      `<div class="ago-yhead" aria-hidden="true">${this.fmt.weekdayNarrow.format(addDays(week0, i))}</div>`).join('');

    let html = '';
    for (let m = 0; m < 12; m++) {
      const first = new Date(y, m, 1);
      const lead = (first.getDay() - fdw + 7) % 7, days = new Date(y, m + 1, 0).getDate();
      let cells = '<div></div>'.repeat(lead);
      for (let d = 1; d <= days; d++) {
        const date = new Date(y, m, d), i = this.dates.push(date) - 1;
        // Week number at the start of each row (the 1st's row starts with empty cells).
        if (wn && (d === 1 || (lead + d - 1) % 7 === 0)) {
          const n = this._weekNumber(date);
          const btn = `<button type="button" class="ago-ywk" data-week="${i}" title="Week ${n}" aria-label="Week ${n}">${n}</button>`;
          cells = d === 1 ? btn + cells : cells + btn;
        }
        cells += `<button type="button" class="ago-yday${sameDay(date, now) ? ' ago-today' : ''}" data-goto="${i}">` +
          `<span class="ago-yday-num">${d}</span><span class="ago-ydots"></span></button>`;
      }
      html += `<section class="ago-ymonth">
        <button type="button" class="ago-ymonth-name" data-month="${m}">${this.fmt.monthLong.format(first)}</button>
        <div class="ago-ygrid${wn ? ' ago-has-wk' : ''}">${heads}${cells}</div>
      </section>`;
    }
    this.grid.innerHTML = `<div class="ago-year">${html}</div>`;
    this.ydays = [...this.grid.querySelectorAll('.ago-yday')];
  }

  _renderYearEvents() {
    // Bucket events by day once instead of scanning every event for each of the 365 days.
    const y = this.date.getFullYear(), yearStart = new Date(y, 0, 1), yearEnd = new Date(y + 1, 0, 1);
    const byDay = this.dates.map(() => []);
    for (const ev of this._shownEvents) {
      if (isBg(ev)) continue;
      if (ev.start >= yearEnd || (ev.end <= yearStart && !sameDay(ev.start, yearStart))) continue;
      let d = ev.start < yearStart ? yearStart : atMinutes(ev.start, 0);
      do {
        const i = Math.round((d - yearStart) / 864e5);
        if (i >= 0 && i < byDay.length) byDay[i].push(ev);
        d = addDays(d, 1);
      } while (d < ev.end && d < yearEnd);
    }

    this.ydays.forEach((el, i) => {
      const items = byDay[i].sort((a, b) => a.start - b.start);
      el.classList.toggle('has-events', items.length > 0);
      el.lastChild.replaceChildren(...items.slice(0, 3).map(ev => {
        const dot = document.createElement('i');
        dot.style.setProperty('--ago-ev', this._colorOf(ev));
        return dot;
      }));
      const label = this.fmt.dayLong.format(this.dates[i]);
      el.title = items.length ? items.map(ev => ev.title).join('\n') : '';
      el.setAttribute('aria-label', items.length ? `${label}, ${items.length} event${items.length > 1 ? 's' : ''}` : label);
    });
  }

  _inCol(ev, i) {
    const r = this.colRes[i];
    return !ev.allDay && !isBg(ev) && sameDay(ev.start, this.dates[i]) && (!r || ev.resourceId === r.id);
  }

  // Events of the calendars and categories left checked in the Filter menu. An event with no
  // calendar or category, or one that is not in the lists, always shows.
  // Recurring events are expanded into their occurrences over the dates on screen.
  get _shownEvents() {
    const { calendar, category } = this.hidden, all = this._occurrences();
    if (!calendar.size && !category.size) return all;
    return all.filter(ev => !calendar.has(ev.calendarId) && !category.has(ev.categoryId));
  }

  get _bgEvents() { return this._shownEvents.filter(isBg); }

  // A background block: shaded with the event's colour, placed by `position`, with the title
  // when `label` is a string (extra classes for the label).
  _bgEl(ev, position, label) {
    const el = div('ago-bgevent' + (ev.display === 'inverse-background' ? ' ago-bgevent-inverse' : ''));
    el.style.setProperty('--ago-ev', this._colorOf(ev));
    position(el);
    if (label != null && ev.title) el.append(div(`ago-bgevent-title ${label}`.trim(), ev.title));
    return el;
  }

  // Background blocks of time-grid column i: each background event over its hours that day,
  // and the hours no inverse-background event covers. All-day ones cover the whole column.
  _bgColumn(i, place) {
    const day = this.dates[i], next = addDays(day, 1), r = this.colRes[i];
    const bgs = this._bgEvents.filter(ev => !r || !ev.resourceId || ev.resourceId === r.id);
    const over = ev => ev.start < next && ev.end > day;
    const span = ev => [ev.start <= day ? 0 : minutesOf(ev.start), ev.end >= next ? 1440 : minutesOf(ev.end)];
    const out = bgs.filter(ev => ev.display === 'background' && over(ev))
      .map(ev => this._bgEl(ev, el => place(el, ...span(ev)), ''));
    const inv = bgs.filter(ev => ev.display === 'inverse-background');
    if (inv.length) {
      for (const [s, e] of gaps(inv.filter(over).map(span), this.startMin, this.endMin)) out.push(this._bgEl(inv[0], el => place(el, s, e)));
    }
    return out;
  }

  // Occurrences are cached until the events or the dates on screen change, so an occurrence being
  // dragged stays the same object between renders.
  _occurrences() {
    const from = atMinutes(this.dates[0], 0), to = addDays(this.dates.at(-1), 1), key = `${+from}|${+to}`;
    if (this._occDirty !== false || this._occKey !== key) {
      this._occ = this._expand(from, to);
      this._occKey = key;
      this._occDirty = false;
    }
    return this._occ;
  }

  _expand(from, to) {
    return this.events.flatMap(ev => (ev.recurrence ? [...Agendo._occurrencesOf(ev, from, to)] : [ev]));
  }

  // An event or occurrence on screen, by id.
  _findShown(id) {
    return this._occurrences().find(x => x.id === id) ?? this.events.find(x => x.id === id);
  }

  // Events changed: expand again and redraw.
  _changed() {
    this._occDirty = true;
    this._renderEvents();
  }

  // Title element, with ↻ before the title of an occurrence of a recurring event.
  _title(ev, className, tag = 'div') {
    const el = Object.assign(document.createElement(tag), { className, textContent: ev.title });
    if (ev.recurringId) {
      const icon = Object.assign(document.createElement('span'), { className: 'ago-rec', textContent: '↻', title: 'Repeats' });
      icon.setAttribute('aria-label', 'Repeats');
      el.prepend(icon);
    }
    return el;
  }

  _isResourceView() { return !!this.tl || this._viewInfo().byRes; }

  // Event colour, exposed to CSS as --ago-ev so themes decide how to paint it.
  _colorOf(ev) {
    const find = (list, id) => id != null && list.find(x => x.id === id)?.color;
    return ev.color || find(this.categories, ev.categoryId) || find(this.calendars, ev.calendarId)
      || find(this.resources, ev.resourceId) || '';
  }

  // Month grid: whole weeks covering the month of this.date (4 to 6 rows).
  _renderMonth() {
    const now = new Date();
    const first = new Date(this.date.getFullYear(), this.date.getMonth(), 1);
    const daysInMonth = new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate();
    const lead = (first.getDay() - this.o.firstDayOfWeek + 7) % 7;
    const weeks = Math.ceil((lead + daysInMonth) / 7);
    this.dates = Array.from({ length: weeks * 7 }, (_, i) => addDays(first, i - lead));
    this.titleEl.textContent = this.fmt.month.format(first);

    this.grid.style.setProperty('--ago-weeks', weeks);
    // Week numbers: a narrow first column; a click opens that week.
    const wn = this.o.weekNumbers;
    this.grid.classList.toggle('ago-has-wk', !!wn);
    const wkCell = i => {
      if (!wn || i % 7) return '';
      const n = this._weekNumber(this.dates[i]);
      return `<button type="button" class="ago-wk-cell" data-week="${i}" title="Week ${n}" aria-label="Week ${n}">${n}</button>`;
    };
    this.grid.innerHTML =
      (wn ? '<div class="ago-month-head ago-col0 ago-wk-head"></div>' : '') +
      this.dates.slice(0, 7).map((d, i) => `<div class="ago-month-head${!wn && !i ? ' ago-col0' : ''}">${this.fmt.weekday.format(d)}</div>`).join('') +
      this.dates.map((d, i) => `${wkCell(i)}
        <div class="ago-cell${!wn && i % 7 === 0 ? ' ago-col0' : ''}${d.getMonth() !== first.getMonth() ? ' ago-other' : ''}${sameDay(d, now) ? ' ago-today' : ''}" data-day="${i}">
          <div class="ago-cell-bg"></div>
          <button type="button" class="ago-cell-num" data-goto="${i}">${d.getDate()}</button>
          <div class="ago-cell-events"></div>
        </div>`).join('');

    this.cols = [];
    this.cells = [...this.grid.querySelectorAll('.ago-cell')];
  }

  _renderMonthEvents() {
    const max = Math.max(1, this.o.monthMaxEvents);
    this.cells.forEach((cell, i) => {
      const day = this.dates[i], next = addDays(day, 1);
      // An event shows on every day it overlaps (zero-length events on their start day).
      const items = this._shownEvents
        .filter(ev => !isBg(ev) && ev.start < next && (ev.end > day || sameDay(ev.start, day)))
        .sort((a, b) => b.allDay - a.allDay || a.start - b.start || b.end - a.end);

      // Background events shade the day when they cover all of it (or are all-day);
      // inverse ones shade the days they do not touch.
      const over = ev => ev.start < next && ev.end > day;
      const bgs = this._bgEvents, inv = bgs.filter(ev => ev.display === 'inverse-background');
      const shade = bgs.filter(ev => ev.display === 'background' && over(ev) && (ev.allDay || (ev.start <= day && ev.end >= next)))
        .map(ev => this._bgEl(ev, () => {}, ''));
      if (inv.length && !inv.some(over)) shade.push(this._bgEl(inv[0], () => {}));
      cell.firstElementChild.replaceChildren(...shade);
      const shown = items.length > max ? items.slice(0, max - 1) : items;

      const els = shown.map(ev => {
        const el = div('ago-mevent' + (this.drag?.event === ev ? ' is-dragging' : ''));
        el.dataset.id = ev.id;
        el.style.setProperty('--ago-ev', this._colorOf(ev));
        const time = !ev.allDay && ev.start >= day ? this.fmt.time.format(ev.start) : '';
        if (time) el.append(Object.assign(document.createElement('span'), { className: 'ago-mevent-time', textContent: time }));
        el.append(this._title(ev, 'ago-mevent-title', 'span'));
        if (!this.o.eventPopover) el.title = `${time} ${ev.title}`.trim();
        return el;
      });
      if (shown.length < items.length) {
        els.push(Object.assign(document.createElement('button'), {
          type: 'button', className: 'ago-more', textContent: `+${items.length - shown.length} more`
        }));
        els.at(-1).dataset.goto = i;
      }
      cell.lastElementChild.replaceChildren(...els);
      cell.classList.toggle('ago-drop', this.drag?.moved === true && this.drag.target === i && this.drag.target !== this.drag.day);
    });
  }

  _renderEvents() {
    if (this._isMonth) return this._renderMonthEvents();
    if (this.list) return this._renderListEvents();
    if (this._kind === 'year') return this._renderYearEvents();
    if (this.tl) return this._renderTimelineEvents();
    const pct = min => ((clamp(min, this.startMin, this.endMin) - this.startMin) / (this.endMin - this.startMin)) * 100;
    const place = (el, s, e) => Object.assign(el.style, { top: `${pct(s)}%`, height: `${pct(e) - pct(s)}%` });
    const handles = !this.o.readonly && this.o.enableResize;
    const hourPx = this.o.slotHeight * this.o.slotsPerHour;
    const preview = this.drag?.mode === 'create' && this.drag.event;

    this.cols.forEach((col, i) => {
      const layer = col.firstElementChild;
      layer.replaceChildren(...this._bgColumn(i, place));

      const items = this._shownEvents
        .filter(ev => this._inCol(ev, i))
        .map(ev => ({ ev, s: minutesOf(ev.start), e: minutesOf(ev.end) || 1440 }))
        .sort((a, b) => a.s - b.s || b.e - a.e);
      this._assignColumns(items);

      for (const { ev, s, e, col: c, cols } of items) {
        // Too short for two lines (time, then title): one line, title first.
        const px = ((clamp(Math.max(e, s + 15), this.startMin, this.endMin) - clamp(s, this.startMin, this.endMin)) / 60) * hourPx;
        const el = div('ago-event' + (this.drag?.event === ev ? ' is-dragging' : '') +
          (px < 38 ? ' ago-compact' : '') + (px < 20 ? ' ago-tiny' : ''));
        el.dataset.id = ev.id;
        const w = 100 / cols;
        place(el, s, Math.max(e, s + 15));
        Object.assign(el.style, { left: `calc(${c * w}% + 2px)`, width: `calc(${w}% - 4px)` });
        el.style.setProperty('--ago-ev', this._colorOf(ev));
        if (handles) {
          el.append(div('ago-handle'), div('ago-handle'));
          el.children[0].dataset.edge = 'top';
          el.children[1].dataset.edge = 'bottom';
        }
        el.append(
          div('ago-event-time', px < 38 ? this.fmt.time.format(ev.start) : `${this.fmt.time.format(ev.start)} – ${this.fmt.time.format(ev.end)}`),
          this._title(ev, 'ago-event-title'));
        layer.append(el);
      }

      if (preview && this._inCol(preview, i)) {
        const p = div('ago-preview');
        place(p, minutesOf(preview.start), minutesOf(preview.end) || 1440);
        layer.append(p);
      }
    });
    this._renderAllDay();
  }

  // All-day row: bars across the columns of the days they cover (split where the columns are not
  // next to each other, as in dates-above-resources views), stacked in lanes. Past three lanes,
  // the third shows "+N" per day, which opens that day.
  _renderAllDay() {
    const row = this.adRow;
    if (!row) return;
    const N = this.dates.length, cap = N > 1 ? 3 : Infinity;
    const handles = !this.o.readonly && this.o.enableResize;
    const preview = this.drag?.ad && this.drag.mode === 'create' && this.drag.event.start && this.drag.event;
    const runs = ev => {
      const out = [];
      let a = -1;
      for (let i = 0; i <= N; i++) {
        const on = i < N && this.dates[i] < ev.end && addDays(this.dates[i], 1) > ev.start &&
          (!this.colRes[i] || ev.resourceId === this.colRes[i].id || (isBg(ev) && !ev.resourceId));
        if (on && a < 0) a = i;
        if (!on && a >= 0) { out.push([a, i - 1]); a = -1; }
      }
      return out;
    };

    const segs = [];
    for (const ev of this._shownEvents) if (ev.allDay && !isBg(ev)) for (const [a, b] of runs(ev)) segs.push({ ev, a, b });
    const bgEls = [];
    for (const ev of this._bgEvents) {
      if (!ev.allDay || ev.display !== 'background') continue;
      for (const [a, b] of runs(ev)) {
        bgEls.push(this._bgEl(ev, el => Object.assign(el.style, { left: `${(a / N) * 100}%`, width: `${((b - a + 1) / N) * 100}%` }), ''));
      }
    }
    segs.sort((x, y) => x.a - y.a || (y.b - y.a) - (x.b - x.a) || x.ev.start - y.ev.start);
    const laneEnds = [];
    for (const s of segs) {
      let lane = laneEnds.findIndex(end => end < s.a);
      if (lane < 0) lane = laneEnds.length;
      laneEnds[lane] = s.b;
      s.lane = lane;
    }
    const limited = laneEnds.length > cap, shownLanes = limited ? cap - 1 : laneEnds.length;
    const pos = (el, a, b, lane) => Object.assign(el.style, {
      left: `calc(${(a / N) * 100}% + 2px)`,
      width: `calc(${((b - a + 1) / N) * 100}% - 4px)`,
      top: `calc(${lane} * var(--ago-ad-lane-h) + 3px)`
    });

    const els = [];
    for (const { ev, a, b, lane } of segs) {
      if (lane >= shownLanes) continue;
      const el = div('ago-event ago-ad-event' + (this.drag?.event === ev ? ' is-dragging' : '') +
        (ev.start < this.dates[a] ? ' ago-clip-start' : '') + (ev.end > addDays(this.dates[b], 1) ? ' ago-clip-end' : ''));
      el.dataset.id = ev.id;
      if (!this.o.eventPopover) el.title = `${ev.title} (all day)`;
      el.style.setProperty('--ago-ev', this._colorOf(ev));
      pos(el, a, b, lane);
      if (handles) {
        el.append(div('ago-handle'), div('ago-handle'));
        el.children[0].dataset.edge = 'start';
        el.children[1].dataset.edge = 'end';
      }
      el.append(this._title(ev, 'ago-event-title'));
      els.push(el);
    }
    if (limited) {
      for (let i = 0; i < N; i++) {
        const hidden = segs.filter(s => s.lane >= shownLanes && s.a <= i && s.b >= i).length;
        if (!hidden) continue;
        const more = Object.assign(document.createElement('button'), { type: 'button', className: 'ago-ad-more', textContent: `+${hidden}` });
        more.dataset.goto = i;
        pos(more, i, i, shownLanes);
        els.push(more);
      }
    }
    if (preview) {
      for (const [a, b] of runs(preview)) {
        const p = div('ago-preview ago-ad-preview');
        pos(p, a, b, 0);
        els.push(p);
      }
    }
    row.style.setProperty('--ago-ad-lanes', Math.max(1, limited ? cap : laneEnds.length));
    row.replaceChildren(...bgEls, ...els);
  }

  // All-day row: drag to create over days, drag a bar sideways to move it, drag its ends to resize.
  _onAdDown(e) {
    const row = this.adRow;
    if (this.o.readonly || e.button !== 0 || e.target.closest('[data-goto]')) return;
    const evEl = e.target.closest('.ago-event');
    const edge = e.target.closest('.ago-handle')?.dataset.edge;
    const event = evEl && this._findShown(evEl.dataset.id);
    const mode = evEl ? (edge || 'move') : 'create';
    if (mode === 'create' && (!this.o.enableCreate || !this.o.enableAllDay)) return;
    const rect = row.getBoundingClientRect();
    this.drag = {
      ad: true, mode, rect, x: e.clientX, y: e.clientY, moved: false,
      col0: this._adCol(e.clientX, rect),
      event: event ?? { allDay: true, start: null, end: null },
      orig: event && { start: event.start, end: event.end, resourceId: event.resourceId }
    };
    this.grid.setPointerCapture(e.pointerId);
    if (mode === 'create') this._onAdMove(e, true);
  }

  _adCol(x, rect) {
    return clamp(Math.floor(((x - rect.left) / rect.width) * this.dates.length), 0, this.dates.length - 1);
  }

  _onAdMove(e, force = false) {
    const d = this.drag;
    if (!d.moved && !force && Math.hypot(e.clientX - d.x, e.clientY - d.y) < 4) return;
    d.moved = !force || d.moved;
    const ev = d.event, c = this._adCol(e.clientX, d.rect), D = this.dates;
    switch (d.mode) {
      case 'create': {
        const a = Math.min(d.col0, c), b = Math.max(d.col0, c);
        ev.start = atMinutes(D[a], 0);
        ev.end = addDays(D[b], 1);
        ev.resourceId = this.colRes[d.col0]?.id;
        break;
      }
      case 'move': {
        if (!this.o.enableMove) return;
        const shift = dayDiff(D[d.col0], D[c]);
        ev.start = addDays(d.orig.start, shift);
        ev.end = addDays(d.orig.end, shift);
        if (this.colRes[c]) ev.resourceId = this.colRes[c].id;
        break;
      }
      case 'start': {
        const s = atMinutes(D[c], 0);
        ev.start = s < d.orig.end ? s : addDays(d.orig.end, -1);
        break;
      }
      case 'end': {
        const en = addDays(D[c], 1);
        ev.end = en > d.orig.start ? en : addDays(d.orig.start, 1);
        break;
      }
    }
    this._renderEvents();
  }

  // Side-by-side layout: within each cluster of overlapping events, put each event
  // in the first sub-column that's free, then give the whole cluster the same width.
  _assignColumns(items) {
    let cluster = [], colEnds = [], clusterEnd = -1;
    const flush = () => cluster.forEach(it => (it.cols = colEnds.length));
    for (const it of items) {
      if (it.s >= clusterEnd) { flush(); cluster = []; colEnds = []; }
      let c = colEnds.findIndex(end => end <= it.s);
      if (c < 0) c = colEnds.length;
      colEnds[c] = it.e;
      it.col = c;
      cluster.push(it);
      clusterEnd = Math.max(clusterEnd, it.e);
    }
    flush();
  }

  _renderNow() {
    this.grid.querySelectorAll('.ago-now, .ago-tl-now-dot').forEach(el => el.remove());
    if (!this.o.showCurrentTime) return;
    const now = new Date(), min = minutesOf(now);
    if (this.tl) {
      const t = this.tl;
      if (now < t.first || now >= addDays(t.first, t.days) || min < t.startMin || min > t.endMin) return;
      const left = `${this._tlX(now) * 100}%`;
      this.rows.forEach(row => row.append(Object.assign(div('ago-now ago-tl-now'), { style: `left: ${left}` })));
      this.grid.querySelector('.ago-tl-head').append(Object.assign(div('ago-tl-now-dot'), { style: `left: ${left}` }));
      return;
    }
    if (this._isMonth || min < this.startMin || min > this.endMin) return;
    const top = `${((min - this.startMin) / (this.endMin - this.startMin)) * 100}%`;
    this.cols.forEach((col, i) => {
      if (sameDay(this.dates[i], now)) col.append(Object.assign(div('ago-now'), { style: `top: ${top}` }));
    });
  }

  // --- Pointer interaction (create / move / resize) ---

  _onDown(e) {
    if (this._isMonth) return this._onMonthDown(e);
    if (this.tl) return this._onTlDown(e);
    if (e.target.closest('.ago-allday')) return this._onAdDown(e);
    const col = e.target.closest('.ago-col');
    if (this.o.readonly || e.button !== 0 || !col) return;

    const evEl = e.target.closest('.ago-event');
    const edge = e.target.closest('.ago-handle')?.dataset.edge;
    const event = evEl && this._findShown(evEl.dataset.id);
    const mode = evEl ? (edge || 'move') : 'create';
    if (mode === 'create' && !this.o.enableCreate) return;

    const rect = col.getBoundingClientRect(), day = +col.dataset.day;
    this.drag = {
      mode, day, rect, x: e.clientX, y: e.clientY, moved: false,
      anchor: this._snap(this._yToMin(e.clientY, rect), Math.floor),
      event: event ?? { start: null, end: null },
      orig: event && { start: event.start, end: event.end, resourceId: event.resourceId }
    };
    this.grid.setPointerCapture(e.pointerId);
    if (mode === 'create') this._onMove(e, true);
  }

  _onMove(e, force = false) {
    const d = this.drag;
    if (!d) return;
    if (d.mode === 'month') return this._onMonthMove(e);
    if (d.tl) return this._onTlMove(e);
    if (d.ad) return this._onAdMove(e);
    const dx = e.clientX - d.x, dy = e.clientY - d.y;
    if (!d.moved && !force && Math.hypot(dx, dy) < 4) return;
    d.moved = !force || d.moved;

    const ev = d.event, slot = this.slot, min = this._yToMin(e.clientY, d.rect);
    switch (d.mode) {
      case 'create': {
        const cur = this._snap(min, Math.floor);
        ev.start = atMinutes(this.dates[d.day], Math.min(d.anchor, cur));
        ev.end = atMinutes(this.dates[d.day], Math.max(d.anchor, cur) + slot);
        ev.resourceId = this.colRes[d.day]?.id;
        break;
      }
      case 'move': {
        if (!this.o.enableMove) return;
        const dur = (d.orig.end - d.orig.start) / 60_000;
        const day = clamp(d.day + Math.round(dx / d.rect.width), 0, this.dates.length - 1);
        const shift = this._snap((dy / d.rect.height) * (this.endMin - this.startMin));
        const start = clamp(minutesOf(d.orig.start) + shift, this.startMin, this.endMin - dur);
        ev.start = atMinutes(this.dates[day], start);
        ev.end = new Date(+ev.start + dur * 60_000);
        if (this.colRes[day]) ev.resourceId = this.colRes[day].id;  // sideways drag changes resource
        break;
      }
      case 'top':
        ev.start = atMinutes(ev.start, Math.min(this._snap(min), minutesOf(d.orig.end) - slot));
        break;
      case 'bottom':
        ev.end = atMinutes(ev.end, Math.max(this._snap(min), minutesOf(d.orig.start) + slot));
        break;
    }
    this._renderEvents();
  }

  _onUp(e) {
    const d = this.drag;
    if (!d) return;
    this.drag = null;
    if (d.mode === 'month') return this._onMonthUp(e, d);

    if (e.type === 'pointercancel') {
      if (d.orig) Object.assign(d.event, d.orig);
    } else if (d.mode === 'create') {
      this.openCreateModal(d.event.start, d.event.end, d.event.resourceId, !!d.event.allDay);
    } else if (!d.moved) {
      this.o.onEventClick ? this.o.onEventClick(d.event) : this.openEditModal(d.event);
    } else if (d.event.recurringId) {
      const ev = d.event, orig = d.orig;
      this._commitOccurrence(ev, { start: ev.start, end: ev.end, resourceId: ev.resourceId },
        d.mode === 'move' ? 'move' : 'resize', () => Object.assign(ev, orig));
    } else {
      this.o.onEventChange?.(d.event, d.mode === 'move' ? 'move' : 'resize');
    }
    this._renderEvents();
  }

  // Month view: click an empty cell to create, click an event to edit, drag an event to another day.
  _onMonthDown(e) {
    const cell = e.target.closest('.ago-cell');
    if (this.o.readonly || e.button !== 0 || !cell || e.target.closest('[data-goto]')) return;
    const evEl = e.target.closest('.ago-mevent');
    const event = evEl && this._findShown(evEl.dataset.id);
    if (!event && !this.o.enableCreate) return;
    const day = +cell.dataset.day;
    this.drag = { mode: 'month', event, day, target: day, x: e.clientX, y: e.clientY, moved: false };
    this.grid.setPointerCapture(e.pointerId);
  }

  _onMonthMove(e) {
    const d = this.drag;
    if (!d.event || !this.o.enableMove) return;
    if (!d.moved && Math.hypot(e.clientX - d.x, e.clientY - d.y) < 4) return;
    d.moved = true;
    const cell = document.elementFromPoint(e.clientX, e.clientY)?.closest('.ago-cell');
    if (cell && this.grid.contains(cell)) d.target = +cell.dataset.day;
    this._renderEvents();
  }

  _onMonthUp(e, d) {
    const ev = d.event;
    if (e.type === 'pointercancel') {
      // nothing was changed yet
    } else if (!ev) {
      const min = (this.o.businessHours ? this.o.businessHours.start : this.o.startHour) * 60;
      const day = this.dates[d.day];
      this.openCreateModal(atMinutes(day, min), atMinutes(day, min + 60));
    } else if (!d.moved) {
      this.o.onEventClick ? this.o.onEventClick(ev) : this.openEditModal(ev);
    } else if (d.target !== d.day && ev.recurringId) {
      const shift = d.target - d.day;
      this._commitOccurrence(ev, { start: addDays(ev.start, shift), end: addDays(ev.end, shift) }, 'move');
    } else if (d.target !== d.day) {
      const shift = d.target - d.day;
      ev.start = addDays(ev.start, shift);
      ev.end = addDays(ev.end, shift);
      this.o.onEventChange?.(ev, 'move');
    }
    this._renderEvents();
  }

  _defaultHour() {
    return this.o.businessHours ? this.o.businessHours.start - 1 : 8;
  }

  _yToMin(y, rect) {
    return this.startMin + (clamp(y - rect.top, 0, rect.height) / rect.height) * (this.endMin - this.startMin);
  }

  _snap(min, round = Math.round) {
    return round(min / this.slot) * this.slot;
  }

  // Week number of `date` (see weekNumberCalculation).
  _weekNumber(date) {
    const c = this.o.weekNumberCalculation;
    if (typeof c === 'function') return c(new Date(date));
    if (c === 'ISO' || (c !== 'local' && this.o.firstDayOfWeek === 1)) {
      // ISO 8601: weeks start on Monday; week 1 holds the year's first Thursday.
      const thu = addDays(atMinutes(date, 0), 3 - ((date.getDay() + 6) % 7));
      return 1 + Math.floor(dayDiff(new Date(thu.getFullYear(), 0, 1), thu) / 7);
    }
    // Local: weeks start on firstDayOfWeek; week 1 is the one holding 1 January.
    const start = this._weekStart(date), year = addDays(start, 6).getFullYear();
    return 1 + Math.round(dayDiff(this._weekStart(new Date(year, 0, 1)), start) / 7);
  }

  _weekLabel(date) {
    const n = this._weekNumber(date);
    return `<span class="ago-weeknum" title="Week ${n}">W${n}</span>`;
  }

  _weekStart(date) {
    const d = atMinutes(date, 0);
    return addDays(d, -((d.getDay() - this.o.firstDayOfWeek + 7) % 7));
  }

  // --- Dialog ---

  // --- Hover popover: the event's details ---

  _bindPopover() {
    const pop = this.pop = this.el.querySelector('.ago-pop');
    if (!pop.showPopover) pop.hidden = true;   // no Popover API: a fixed element shown with `hidden`
    const sel = '.ago-event, .ago-mevent, .ago-list-item';
    this.grid.addEventListener('pointerover', e => {
      const el = e.target.closest(sel);
      if (!el || el === this._popFor || e.pointerType !== 'mouse' || !this.o.eventPopover || this.drag) return;
      this._hidePopover();
      this._popFor = el;
      this._popTimer = setTimeout(() => this._showPopover(el), 400);
    });
    this.grid.addEventListener('pointerout', e => {
      const el = e.target.closest(sel);
      if (el && !el.contains(e.relatedTarget)) this._hidePopover();
    });
    // Before a drag starts, and when the view scrolls away from the event.
    this.grid.addEventListener('pointerdown', () => this._hidePopover(), true);
    this.grid.addEventListener('scroll', () => this._hidePopover(), { passive: true });
  }

  _hidePopover() {
    clearTimeout(this._popTimer);
    this._popFor = null;
    const pop = this.pop;
    if (!pop) return;
    if (pop.showPopover) { if (pop.matches(':popover-open')) pop.hidePopover(); } else pop.hidden = true;
  }

  _showPopover(el) {
    const ev = el.isConnected && !this.drag && this._findShown(el.dataset.id);
    if (!ev) return;
    const pop = this.pop, custom = this.o.eventPopover;
    if (typeof custom === 'function') {
      const c = custom(ev);
      if (c == null) return;
      pop.replaceChildren(typeof c === 'string' ? document.createTextNode(c) : c);
    } else pop.replaceChildren(...this._popoverContent(ev));
    pop.style.setProperty('--ago-ev', this._colorOf(ev) || 'var(--ago-event-bg)');
    if (pop.showPopover) pop.showPopover(); else pop.hidden = false;

    // Beside the event when there is room (right, else left), else below or above it.
    const r = el.getBoundingClientRect(), p = pop.getBoundingClientRect();
    const vw = document.documentElement.clientWidth, vh = document.documentElement.clientHeight, gap = 8;
    let x = r.right + gap, y = r.top;
    if (x + p.width > vw - gap) x = r.left - gap - p.width;
    if (x < gap) {
      x = clamp(r.left, gap, vw - p.width - gap);
      y = r.bottom + gap + p.height <= vh - gap ? r.bottom + gap : r.top - gap - p.height;
    }
    pop.style.left = `${Math.max(gap, x)}px`;
    pop.style.top = `${clamp(y, gap, Math.max(gap, vh - p.height - gap))}px`;
  }

  _popoverContent(ev) {
    const f = this.fmt, t = d => f.time.format(d), parts = [this._title(ev, 'ago-pop-title')];
    let when;
    if (ev.allDay) {
      const last = addDays(ev.end, -1);
      when = [sameDay(ev.start, last) ? f.dayLong.format(ev.start) : f.range.formatRange(ev.start, last), 'All day'];
    } else if (sameDay(ev.start, ev.end) || +ev.end === +addDays(atMinutes(ev.start, 0), 1)) {
      when = [f.dayLong.format(ev.start), `${t(ev.start)} – ${t(ev.end)}`];
    } else {
      when = [`${f.dayShort.format(ev.start)}, ${t(ev.start)} – ${f.dayShort.format(ev.end)}, ${t(ev.end)}`];
    }
    parts.push(...when.map(w => div('ago-pop-when', w)));
    if (ev.recurrence) {
      const series = this.events.find(x => x.id === ev.recurringId);
      parts.push(div('ago-pop-rule', `↻ ${this._describeRule(ev.recurrence, series?.start ?? ev.start)}`));
    }

    const rows = [];
    const add = (label, list, id) => {
      const item = id != null && list.find(x => x.id === id);
      if (!item) return;
      const dd = document.createElement('dd');
      if (item.color) dd.append(Object.assign(document.createElement('span'), { className: 'ago-res-dot', style: `background:${item.color}` }));
      dd.append(item.title);
      rows.push(Object.assign(document.createElement('dt'), { textContent: label }), dd);
    };
    add('Resource', this.resources, ev.resourceId);
    add('Calendar', this.calendars, ev.calendarId);
    add('Category', this.categories, ev.categoryId);
    if (ev.location) {
      rows.push(Object.assign(document.createElement('dt'), { textContent: 'Location' }),
        Object.assign(document.createElement('dd'), { textContent: ev.location }));
    }
    if (rows.length) {
      const dl = document.createElement('dl');
      dl.append(...rows);
      parts.push(dl);
    }
    if (ev.description) parts.push(div('ago-pop-desc', ev.description));
    return parts;
  }

  // "Weekly on Monday, Wednesday", "Every 2 months on the last Friday, until 30 June 2027"…
  _describeRule(r, start) {
    const f = this.fmt, iv = r.interval || 1;
    const name = code => f.weekdayLong.format(addDays(new Date(2023, 0, 1), WD.indexOf(code)));   // 1 Jan 2023 = Sunday
    const ordinal = n => (n < 0 ? 'last' : ['first', 'second', 'third', 'fourth', 'fifth'][n - 1]);
    const units = { daily: 'days', weekly: 'weeks', monthly: 'months', yearly: 'years' };
    let s = iv === 1 ? { daily: 'Daily', weekly: 'Weekly', monthly: 'Monthly', yearly: 'Yearly' }[r.freq] : `Every ${iv} ${units[r.freq]}`;
    if (r.freq === 'weekly') {
      const days = r.byDay ?? [WD[start.getDay()]];
      s += days.join(',') === 'MO,TU,WE,TH,FR' ? ' on weekdays' : ` on ${days.map(c => name(c.slice(-2))).join(', ')}`;
    } else if (r.freq === 'monthly') {
      const nth = r.byDay?.find(c => parseInt(c, 10));
      s += nth ? ` on the ${ordinal(parseInt(nth, 10))} ${name(nth.slice(-2))}` : ` on day ${start.getDate()}`;
    } else if (r.freq === 'yearly') s += ` on ${f.dayMonth.format(start)}`;
    if (r.until) s += `, until ${f.range.format(parseDay(r.until))}`;
    else if (r.count) s += `, ${r.count} times`;
    return s;
  }

  _openDialog(title, event, start, end, resourceId, allDay = false) {
    const f = this.form.elements;
    this.editing = event;
    this.dialog.querySelector('.ago-dialog-title').textContent = title;
    this.form.querySelector('[value="delete"]').hidden = !event;
    f.title.value = event?.title ?? '';
    f.allDay.checked = event?.allDay ?? allDay;
    this._setDateInputs(f.allDay.checked, start, end);
    // Hidden fields keep their values, so an existing all-day or recurring event stays one.
    this.form.querySelector('[data-allday-field]').hidden = !this.o.enableAllDay;
    this.form.querySelector('[data-repeat]').hidden = !this.o.enableRecurring;
    f.resourceId.value = event?.resourceId ?? resourceId ?? '';
    // The resource field only shows in resource and timeline views. Elsewhere it stays hidden but
    // keeps its value, so a new event gets no resource and an edited event keeps its own.
    this.form.querySelector('[data-res-field]').hidden = !this.resources.length || !this._isResourceView();
    // A new event goes to the first calendar still shown, so it does not vanish on save.
    f.calendarId.value = event?.calendarId
      ?? this.calendars.find(c => !this.hidden.calendar.has(c.id))?.id ?? '';
    f.categoryId.value = event?.categoryId ?? '';
    this._fillRule(event?.recurrence, start);
    this._checkRange();
    this.dialog.showModal();
  }

  openCreateModal(start, end, resourceId, allDay = false) { this._openDialog('New Event', null, start, end, resourceId, allDay); }
  openEditModal(event) { this._openDialog('Edit Event', event, event.start, event.end); }

  // All-day events show their dates, the end date included (stored exclusive, as in iCalendar).
  _setDateInputs(allDay, start, end) {
    const f = this.form.elements;
    f.start.type = f.end.type = allDay ? 'date' : 'datetime-local';
    const last = addDays(end, -1);
    f.start.value = allDay ? dayKey(start) : localDT(start);
    f.end.value = allDay ? dayKey(last < start ? start : last) : localDT(end);
  }

  _readDates() {
    const f = this.form.elements, allDay = f.allDay.checked;
    const read = el => (el.type === 'date' ? parseDay(el.value) : new Date(el.value));
    const start = read(f.start), end = read(f.end);
    return { allDay, start, end: allDay ? addDays(end, 1) : end };
  }

  // All day on: keep the days. Off: business hours start, one hour long.
  _toggleAllDay() {
    const f = this.form.elements, on = f.allDay.checked;
    const read = el => (el.type === 'date' ? parseDay(el.value) : new Date(el.value));
    let s = read(f.start), e = read(f.end);
    if (isNaN(s)) s = new Date();
    if (isNaN(e) || e < s) e = s;
    if (on) {
      const lastDay = minutesOf(e) === 0 && e > s ? addDays(e, -1) : e;   // a time end at midnight ends the day before
      this._setDateInputs(true, atMinutes(s, 0), addDays(atMinutes(lastDay, 0), 1));
    } else {
      const m = (this.o.businessHours?.start ?? 9) * 60;
      this._setDateInputs(false, atMinutes(s, m), atMinutes(e, m + 60));
    }
    this._checkRange();
    this._repeatLabels();
  }

  // --- Dialog: repeat ---

  // Option texts follow the start date: "Weekly on Monday", "Monthly on the fourth Monday"…
  _repeatLabels() {
    const f = this.form.elements, d = this._readDates().start;
    if (isNaN(d)) return;
    const wd = this.fmt.weekdayLong.format(d), n = Math.ceil(d.getDate() / 7);
    const ord = n > 4 ? 'last' : ['first', 'second', 'third', 'fourth'][n - 1];
    const text = {
      none: 'Does not repeat', daily: 'Daily', weekly: `Weekly on ${wd}`,
      weekdays: 'Every weekday (Monday to Friday)', monthly: `Monthly on day ${d.getDate()}`,
      monthlyNth: `Monthly on the ${ord} ${wd}`, yearly: `Yearly on ${this.fmt.dayMonth.format(d)}`, custom: 'Custom…'
    };
    for (const opt of f.repeat.options) opt.textContent = text[opt.value];
  }

  _presetRule(kind, d) {
    const code = WD[d.getDay()], n = Math.ceil(d.getDate() / 7);
    switch (kind) {
      case 'daily': return { freq: 'daily' };
      case 'weekly': return { freq: 'weekly', byDay: [code] };
      case 'weekdays': return { freq: 'weekly', byDay: ['MO', 'TU', 'WE', 'TH', 'FR'] };
      case 'monthly': return { freq: 'monthly' };
      case 'monthlyNth': return { freq: 'monthly', byDay: [n > 4 ? `-1${code}` : `${n}${code}`] };
      case 'yearly': return { freq: 'yearly' };
    }
    return null;
  }

  _fillCustom(rule, start) {
    const f = this.form.elements, days = new Set(rule.byDay ?? [WD[start.getDay()]]);
    f.freq.value = rule.freq;
    f.interval.value = rule.interval ?? 1;
    this.form.querySelectorAll('[name="byDay"]').forEach(b => (b.checked = days.has(b.value)));
  }

  // Rule → form: a preset when it is one, else the custom fields.
  _fillRule(rule, start) {
    const f = this.form.elements, r = Agendo._normRule(rule);
    let kind = 'none';
    if (r) {
      const code = WD[start.getDay()], n = Math.ceil(start.getDate() / 7), by = (r.byDay ?? []).join(',');
      const nth = n > 4 ? `-1${code}` : `${n}${code}`;
      kind = (r.interval ?? 1) !== 1 ? 'custom'
        : r.freq === 'daily' && !by ? 'daily'
        : r.freq === 'weekly' && (!by || by === code) ? 'weekly'
        : r.freq === 'weekly' && by === 'MO,TU,WE,TH,FR' ? 'weekdays'
        : r.freq === 'monthly' && !by ? 'monthly'
        : r.freq === 'monthly' && by === nth ? 'monthlyNth'
        : r.freq === 'yearly' && !by ? 'yearly'
        : 'custom';
    }
    f.repeat.value = kind;
    this._prevRepeat = kind;
    this._fillCustom(r ?? { freq: 'weekly' }, start);
    f.ends.value = r?.until ? 'until' : r?.count ? 'count' : 'never';
    f.until.value = r?.until ?? '';
    f.count.value = r?.count ?? 10;
    this._ruleTouched = false;
    this._repeatLabels();
    this._repeatUI();
  }

  // Form → rule (null = does not repeat).
  _readRule(start) {
    const f = this.form.elements, kind = f.repeat.value;
    if (kind === 'none') return null;
    let r;
    if (kind === 'custom') {
      const days = [...this.form.querySelectorAll('[name="byDay"]:checked')].map(b => b.value);
      r = { freq: f.freq.value, interval: +f.interval.value || 1 };
      if (r.freq === 'weekly') r.byDay = days.length ? days : [WD[start.getDay()]];
    } else r = this._presetRule(kind, start);
    if (f.ends.value === 'until' && f.until.value) r.until = f.until.value;
    if (f.ends.value === 'count') r.count = Math.max(1, parseInt(f.count.value, 10) || 1);
    return Agendo._normRule(r);
  }

  _repeatUI() {
    const f = this.form.elements, kind = f.repeat.value;
    this.form.querySelector('[data-custom]').hidden = kind !== 'custom';
    this.form.querySelector('[data-weekdays]').hidden = f.freq.value !== 'weekly';
    this.form.querySelector('[data-ends]').hidden = kind === 'none';
  }

  _onSubmit(e) {
    const ev = this.editing, f = this.form.elements;
    switch (e.submitter?.value) {
      case 'delete':
        if (ev.recurringId) {
          this._askScope('Delete recurring event').then(scope => scope && this._deleteScoped(ev, scope));
        } else {
          this.deleteEvent(ev.id);
          this.o.onEventDelete?.(ev.id);
        }
        break;
      case 'save': {
        const { allDay, start, end } = this._readDates();
        const data = { title: f.title.value, allDay, start, end };
        if (this.resources.length) data.resourceId = f.resourceId.value || undefined;
        if (this.calendars.length) data.calendarId = f.calendarId.value || undefined;
        if (this.categories.length) data.categoryId = f.categoryId.value || undefined;
        // The repeat fields count only when they were changed, so moving one occurrence to another
        // day does not turn "Weekly on Monday" into "Weekly on Tuesday".
        const rule = this._ruleTouched ? this._readRule(start) : Agendo._normRule(ev?.recurrence) ?? null;

        if (!ev) {
          this.o.onEventCreate?.(this.addEvent({ ...data, recurrence: rule }));
        } else if (!ev.recurringId) {
          const keep = rule && ev.recurrence?.exdates ? { ...rule, exdates: ev.recurrence.exdates } : rule;
          Agendo._assign(ev, { ...ev, ...data, recurrence: keep });
          this._changed();
          this.o.onEventChange?.(ev, 'edit');
        } else if (!rule) {
          // Stops repeating: the series becomes this one event.
          const series = this.events.find(x => x.id === ev.recurringId);
          if (!series) break;
          Agendo._assign(series, { ...series, ...data, recurrence: null });
          this._changed();
          this.o.onEventChange?.(series, 'edit', { scope: 'all', recurringId: series.id, occurrence: ev.occurrence });
        } else {
          const changed = this._ruleTouched && Agendo.toRRule(rule) !== Agendo.toRRule(ev.recurrence);
          this._askScope('Edit recurring event', !changed).then(scope =>
            scope && this._applyScoped(ev, changed ? { ...data, recurrence: rule } : data, scope, 'edit'));
        }
      }
    }
  }

  // --- Recurring events: which occurrences a change applies to ---

  // "This event / This and following events / All events". Resolves to the choice, or null.
  _askScope(title, allowThis = true) {
    const dlg = this.scopeDialog, f = dlg.querySelector('form').elements;
    dlg.querySelector('.ago-dialog-title').textContent = title;
    dlg.querySelector('[data-scope-this]').hidden = !allowThis;
    f.scope.value = allowThis ? 'this' : 'following';
    dlg.returnValue = '';
    dlg.showModal();
    return new Promise(resolve => dlg.addEventListener('close',
      () => resolve(dlg.returnValue === 'ok' ? f.scope.value : null), { once: true }));
  }

  async _commitOccurrence(occ, changes, action, revert) {
    const scope = await this._askScope('Change recurring event');
    if (scope) return this._applyScoped(occ, changes, scope, action);
    revert?.();
    this._renderEvents();
  }

  _newId(base) {
    let id = base, n = 2;
    while (this.events.some(e => e.id === id)) id = `${base}-${n++}`;
    return id;
  }

  // Applies `changes` (start, end and other fields; `recurrence` when the rule itself changed)
  // made to occurrence `occ`:
  //   this      → the date is excluded from the series and a single event takes its place;
  //   following → the series ends the day before and a new series starts with the changes;
  //   all       → the series moves by the same days / minutes and takes the other changes.
  _applyScoped(occ, changes, scope, action) {
    const series = this.events.find(e => e.id === occ.recurringId);
    if (!series) return;
    const r = series.recurrence, key = occ.occurrence;
    const origStart = atMinutes(parseDay(key), minutesOf(series.start));
    if (scope === 'following' && key === dayKey(series.start)) scope = 'all';
    const info = { scope, recurringId: series.id, occurrence: key };
    const { recurrence: newRule, ...fields } = changes;
    const shift = dayDiff(origStart, fields.start);
    const moveKeys = keys => keys && (shift ? keys.map(k => dayKey(addDays(parseDay(k), shift))) : keys);
    // The rule that goes on: the one from the dialog, else the series' own with its weekdays moved.
    const base = newRule ? { ...newRule } : Agendo._shiftRule(r, shift);

    if (scope === 'this') {
      r.exdates = [...new Set([...(r.exdates ?? []), key])].sort();
      const single = Agendo._normalize({ ...series, ...fields, id: this._newId(`${series.id}-${key}`), recurrence: null });
      this.events.push(single);
      this._changed();
      this.o.onEventChange?.(series, 'exclude', info);
      this.o.onEventCreate?.(single, info);
    } else if (scope === 'all') {
      const minShift = minutesOf(fields.start) - minutesOf(origStart);
      const start = atMinutes(addDays(series.start, shift), minutesOf(series.start) + minShift);
      const end = atMinutes(addDays(start, dayDiff(fields.start, fields.end)), minutesOf(fields.end));
      Agendo._assign(series, { ...series, ...fields, start, end, recurrence: { ...base, exdates: moveKeys(r.exdates) } });
      this._changed();
      this.o.onEventChange?.(series, action, info);
    } else {
      const next = { ...base, exdates: moveKeys(r.exdates?.filter(k => k >= key)) };
      if (!newRule && r.count) next.count = Math.max(1, r.count - Agendo._countBefore(series, key));
      series.recurrence = Agendo._endBefore(series, key);
      const tail = Agendo._normalize({ ...series, ...fields, id: this._newId(`${series.id}-${key}`), recurrence: next });
      this.events.push(tail);
      this._changed();
      this.o.onEventChange?.(series, 'truncate', info);
      this.o.onEventCreate?.(tail, info);
    }
  }

  _deleteScoped(occ, scope) {
    const series = this.events.find(e => e.id === occ.recurringId);
    if (!series) return;
    const key = occ.occurrence;
    if (scope === 'following' && key === dayKey(series.start)) scope = 'all';
    const info = { scope, recurringId: series.id, occurrence: key };
    if (scope === 'this') {
      series.recurrence.exdates = [...new Set([...(series.recurrence.exdates ?? []), key])].sort();
      this._changed();
      this.o.onEventChange?.(series, 'exclude', info);
    } else if (scope === 'following') {
      series.recurrence = Agendo._endBefore(series, key);
      this._changed();
      this.o.onEventChange?.(series, 'truncate', info);
    } else {
      this.events = this.events.filter(e => e !== series);
      this._changed();
      this.o.onEventDelete?.(series.id, info);
    }
  }

  // --- Public API ---

  prev() { this._step(-1); }
  next() { this._step(1); }

  _step(dir) {
    if (this.o.daysToShow === 'year') return this.gotoDate(new Date(this.date.getFullYear() + dir, this.date.getMonth(), 1));
    const t = this._tl() ?? this._listRange();
    if (this._isMonth || t?.month) return this.gotoDate(new Date(this.date.getFullYear(), this.date.getMonth() + dir, 1));
    if (t) return this.gotoDate(addDays(this.date, dir * t.jump));
    this.gotoDate(addDays(this.date, dir * (this._viewInfo().days === 1 ? 1 : 7)));
  }

  today() { this.gotoDate(new Date()); }
  gotoDate(date) { this.date = new Date(date); this.render(); }
  setView(daysToShow) { this.o.daysToShow = daysToShow; this.render(); }

  scrollToHour(hour) {
    if (this._kind !== 'grid') return;
    this.grid.scrollTop = Math.max(0, hour - this.o.startHour) * this.o.slotHeight * this.o.slotsPerHour;
  }

  getResources() { return [...this.resources]; }

  setResources(resources) {
    this._applyResources(resources);
    this.render();
  }

  // Resource buttons and the dialog's resource field only show when there are resources.
  _applyResources(resources) {
    this.resources = (resources || []).map(r => ({ ...r, id: String(r.id), title: r.title ?? String(r.id) }));
    this.el.querySelector('[data-res-field]').hidden = !this.resources.length;
    const custom = this.el.querySelector('[data-days="timeline-custom"]'), c = this.o.timelineCustom;
    custom.textContent = c?.label ?? `${c?.days ?? 3} days`;
    this.form.elements.resourceId.innerHTML = '<option value="">—</option>' +
      this.resources.map(r => `<option value="${esc(r.id)}">${esc(r.title)}</option>`).join('');
    this._applyViews();
  }

  getViews() { return [...this.el.querySelectorAll('.ago-views [data-days]')].filter(b => !b.hidden).map(b => b.dataset.days); }

  // Shows only the chosen view buttons. Takes view keys (1, 5, 7, 'month', 'resource-day'…) and the
  // menu names 'list', 'resource' and 'timeline' for a whole dropdown; null shows every view.
  setViews(views) {
    this.o.views = views;
    this._applyViews();
    if (!this._viewAllowed(this.o.daysToShow)) this.o.daysToShow = this._firstView();
    this.render();
  }

  _viewAllowed(view) {
    const v = String(view), w = this.o.views;
    if (/^(resource|timeline)-/.test(v) && !this.resources.length) return false;
    if (v === 'timeline-custom' && !this.o.timelineCustom) return false;
    if (!w) return true;
    const alias = { day: '1', workweek: '5', week: '7', resources: 'resource' };
    return w.some(k => { k = alias[k] ?? String(k); return k === v || v.startsWith(k + '-'); });
  }

  _firstView() {
    const b = [...this.el.querySelectorAll('.ago-views [data-days]')].find(b => this._viewAllowed(b.dataset.days));
    return b ? (isNaN(b.dataset.days) ? b.dataset.days : +b.dataset.days) : this.o.daysToShow;
  }

  // Hides the buttons and menu items left out, then any menu, group or switcher left empty;
  // the switcher also hides when only one view is left.
  _applyViews() {
    const views = this.el.querySelector('.ago-views');
    let shown = 0;
    views.querySelectorAll('[data-days]').forEach(b => { b.hidden = !this._viewAllowed(b.dataset.days); shown += !b.hidden; });
    views.querySelectorAll('.ago-dropdown').forEach(d => (d.hidden = !d.querySelector('[data-days]:not([hidden])')));
    views.querySelectorAll('.ago-group').forEach(g => (g.hidden = !g.querySelector(':scope > :not([hidden])')));
    views.hidden = shown < 2;
  }

  getCalendars() { return [...this.calendars]; }
  getCategories() { return [...this.categories]; }
  setCalendars(calendars) { this._applyFilterLists(calendars, this.categories); this.render(); }
  setCategories(categories) { this._applyFilterLists(this.calendars, categories); this.render(); }

  // Ids of the calendars and categories whose events show.
  getFilter() {
    const shown = (list, kind) => list.filter(x => !this.hidden[kind].has(x.id)).map(x => x.id);
    return { calendars: shown(this.calendars, 'calendar'), categories: shown(this.categories, 'category') };
  }

  // { calendars?: ids, categories?: ids } — the ids to show; a key left out keeps its current state.
  setFilter({ calendars, categories } = {}) {
    const apply = (list, kind, ids) => {
      if (!ids) return;
      const keep = new Set(ids.map(String));
      this.hidden[kind] = new Set(list.filter(x => !keep.has(x.id)).map(x => x.id));
    };
    apply(this.calendars, 'calendar', calendars);
    apply(this.categories, 'category', categories);
    this._filterChanged();
  }

  _filterChanged() {
    this._syncFilterMenu();
    this._renderEvents();
    this.o.onFilterChange?.(this.getFilter());
  }

  // Builds the Filter menu and the dialog's calendar and category fields from the lists.
  _applyFilterLists(calendars, categories) {
    const norm = list => (list || []).map(x => ({ ...x, id: String(x.id), title: x.title ?? String(x.id) }));
    this.calendars = norm(calendars);
    this.categories = norm(categories);
    // Keep the state of items already known, take `visible` from new ones.
    const prev = this.hidden;
    const hide = (list, kind) => new Set(list.filter(x =>
      prev?.[kind].has(x.id) || (x.visible === false && !prev?.known.has(`${kind}:${x.id}`))).map(x => x.id));
    this.hidden = { calendar: hide(this.calendars, 'calendar'), category: hide(this.categories, 'category') };
    this.hidden.known = new Set([...this.calendars.map(x => `calendar:${x.id}`), ...this.categories.map(x => `category:${x.id}`)]);

    const dot = x => `<span class="ago-res-dot"${x.color ? ` style="background:${esc(x.color)}"` : ''}></span>`;
    const section = (list, kind, legend) => list.length ? `<fieldset data-kind="${kind}"><legend>${legend}</legend>${
      list.map(x => `<label><input type="checkbox" value="${esc(x.id)}">${dot(x)}${esc(x.title)}</label>`).join('')}</fieldset>` : '';
    const menu = this.el.querySelector('.ago-filter');
    menu.hidden = !this.calendars.length && !this.categories.length;
    menu.lastElementChild.innerHTML = section(this.calendars, 'calendar', 'Calendars') + section(this.categories, 'category', 'Categories');
    this._syncFilterMenu();

    const opts = list => '<option value="">—</option>' +
      list.map(x => `<option value="${esc(x.id)}">${esc(x.title)}</option>`).join('');
    const f = this.form.elements;
    f.calendarId.innerHTML = opts(this.calendars);
    f.categoryId.innerHTML = opts(this.categories);
    this.form.querySelector('[data-cal-field]').hidden = !this.calendars.length;
    this.form.querySelector('[data-cat-field]').hidden = !this.categories.length;
  }

  // Checkboxes follow the hidden sets; the Filter button is highlighted while something is hidden.
  _syncFilterMenu() {
    const menu = this.el.querySelector('.ago-filter');
    menu.querySelectorAll('[data-kind] input').forEach(box => {
      box.checked = !this.hidden[box.closest('[data-kind]').dataset.kind].has(box.value);
    });
    const off = this.hidden.calendar.size + this.hidden.category.size;
    const sum = menu.querySelector('summary');
    sum.classList.toggle('active', off > 0);
    sum.textContent = off ? `Filter (${off} hidden)` : 'Filter';
  }

  // The events as stored: a recurring event once, with its `recurrence`.
  getEvents() { return [...this.events]; }

  // Every event and occurrence overlapping [start, end).
  getOccurrences(start, end) {
    const s = toDate(start), e = toDate(end);
    return this._expand(s, e).filter(ev => ev.start < e && (ev.end > s || +ev.start === +s));
  }

  setEvents(events) {
    this.events = (events || []).map(Agendo._normalize);
    this._occDirty = true;
    this.render();
  }

  addEvent(event) {
    const ev = Agendo._normalize(event);
    this.events.push(ev);
    this._changed();
    return ev;
  }

  updateEvent(id, updates) {
    const ev = this.events.find(e => e.id === String(id));
    if (!ev) return;
    Agendo._assign(ev, { ...ev, ...updates });
    this._changed();
  }

  deleteEvent(id) {
    this.events = this.events.filter(e => e.id !== String(id));
    this._changed();
  }

  // Switch the theme class; the matching themes/<name>.css must be loaded. null = default look.
  setTheme(name) {
    if (this.theme) this.el.classList.remove(`ago-theme-${this.theme}`);
    this.theme = name || null;
    if (this.theme) this.el.classList.add(`ago-theme-${this.theme}`);
  }

  // Light or dark colours. 'auto' follows the OS setting and its changes. The calendar element also
  // gets data-bs-theme and data-theme, so the Tabler, Bootstrap and Bulma themes switch with it.
  // null removes all that: default light colours, and framework themes follow the page.
  setColorScheme(scheme) {
    this._schemeMq?.removeEventListener('change', this._onSchemeChange);
    this._schemeMq = null;
    this.colorScheme = ['light', 'dark', 'auto'].includes(scheme) ? scheme : null;
    if (this.colorScheme === 'auto') {
      this._schemeMq = matchMedia('(prefers-color-scheme: dark)');
      this._onSchemeChange ??= () => this._applyScheme(this._schemeMq.matches ? 'dark' : 'light');
      this._schemeMq.addEventListener('change', this._onSchemeChange);
      this._onSchemeChange();
    } else this._applyScheme(this.colorScheme);
  }

  // 'light' | 'dark' actually shown, whatever colorScheme says.
  getColorScheme() {
    return this.el.classList.contains('ago-dark') ? 'dark' : 'light';
  }

  _applyScheme(mode) {
    const el = this.el;
    el.classList.toggle('ago-dark', mode === 'dark');
    el.classList.toggle('ago-light', mode === 'light');
    for (const attr of ['data-bs-theme', 'data-theme']) {
      if (mode) el.setAttribute(attr, mode); else el.removeAttribute(attr);
    }
  }

  _closeMenus() {
    this.el.querySelectorAll('.ago-dropdown[open]').forEach(m => (m.open = false));
  }

  destroy() {
    clearInterval(this.timer);
    this._hidePopover();
    document.removeEventListener('pointerdown', this._onDocPointer);
    this.el.replaceChildren();
    this.el.classList.remove('agendo');
    this.setTheme(null);
    this.setColorScheme(null);
  }

  // Dates parsed (a 'YYYY-MM-DD' string is a local date). An all-day event runs from midnight to
  // midnight, its end exclusive; a missing or too early end makes it one day long.
  static _normalize(evt) {
    const allDay = !!evt.allDay;
    let start = toDate(evt.start), end = evt.end == null ? null : toDate(evt.end);
    if (allDay) {
      start = atMinutes(start, 0);
      if (end && !isNaN(end)) end = minutesOf(end) ? addDays(atMinutes(end, 0), 1) : atMinutes(end, 0);
      if (!end || isNaN(end) || end <= start) end = addDays(start, 1);
    } else if (!end || isNaN(end)) end = new Date(+start + 36e5);
    const ev = {
      ...evt,
      id: String(evt.id ?? Math.random().toString(36).slice(2)),
      resourceId: evt.resourceId == null ? undefined : String(evt.resourceId),
      calendarId: evt.calendarId == null ? undefined : String(evt.calendarId),
      categoryId: evt.categoryId == null ? undefined : String(evt.categoryId),
      title: evt.title || (isBg(evt) ? '' : 'Untitled Event'),
      allDay,
      start,
      end
    };
    delete ev.recurringId;   // a copied occurrence becomes an event of its own
    delete ev.occurrence;
    const rule = Agendo._normRule(evt.recurrence);
    if (rule) ev.recurrence = rule; else delete ev.recurrence;
    return ev;
  }

  // Replaces `target`'s fields in place (an event object keeps its identity).
  static _assign(target, data) {
    const n = Agendo._normalize(data);
    for (const k of Object.keys(target)) if (!(k in n)) delete target[k];
    return Object.assign(target, n);
  }

  // --- Recurrence rules: a subset of iCalendar RRULE ---
  // { freq: 'daily' | 'weekly' | 'monthly' | 'yearly', interval?, byDay?: ['MO', …] or for monthly
  //   ['2TU'] (2nd Tuesday) / ['-1FR'] (last Friday), until?: 'YYYY-MM-DD' (included),
  //   count?, exdates?: ['YYYY-MM-DD'] }, or an RRULE string such as 'FREQ=WEEKLY;BYDAY=MO,WE;COUNT=10'.

  static _normRule(rec) {
    if (!rec) return undefined;
    if (typeof rec === 'string') rec = Agendo.parseRRule(rec);
    const freq = String(rec.freq ?? '').toLowerCase();
    if (!['daily', 'weekly', 'monthly', 'yearly'].includes(freq)) return undefined;
    const out = { freq };
    const interval = Math.max(1, parseInt(rec.interval, 10) || 1);
    if (interval > 1) out.interval = interval;
    const byDay = (Array.isArray(rec.byDay) ? rec.byDay : rec.byDay ? String(rec.byDay).split(',') : [])
      .map(s => String(s).trim().toUpperCase()).filter(s => /^(-?[1-5])?(SU|MO|TU|WE|TH|FR|SA)$/.test(s));
    if (byDay.length) out.byDay = [...new Set(byDay)];
    const until = rec.until && toDate(rec.until);
    if (until && !isNaN(until)) out.until = dayKey(until);
    const count = parseInt(rec.count, 10);
    if (count > 0) out.count = count;
    const ex = (rec.exdates ?? []).map(toDate).filter(d => !isNaN(d)).map(dayKey);
    if (ex.length) out.exdates = [...new Set(ex)].sort();
    return out;
  }

  static parseRRule(str) {
    const out = {};
    for (const part of String(str).replace(/^RRULE:/i, '').split(';')) {
      const [k, v] = part.split('=');
      if (!v) continue;
      switch (k.trim().toUpperCase()) {
        case 'FREQ': out.freq = v.toLowerCase(); break;
        case 'INTERVAL': out.interval = +v; break;
        case 'BYDAY': out.byDay = v.split(','); break;
        case 'COUNT': out.count = +v; break;
        case 'UNTIL': {
          const m = /^(\d{4})(\d{2})(\d{2})/.exec(v);
          if (m) out.until = `${m[1]}-${m[2]}-${m[3]}`;
          break;
        }
      }
    }
    return out;
  }

  // Rule → RRULE string (without the exdates, which iCalendar keeps in EXDATE).
  static toRRule(rule) {
    const r = Agendo._normRule(rule);
    if (!r) return '';
    const parts = [`FREQ=${r.freq.toUpperCase()}`];
    if (r.interval) parts.push(`INTERVAL=${r.interval}`);
    if (r.byDay) parts.push(`BYDAY=${r.byDay.join(',')}`);
    if (r.count) parts.push(`COUNT=${r.count}`);
    if (r.until) parts.push(`UNTIL=${r.until.replaceAll('-', '')}`);
    return parts.join(';');
  }

  // Days (local midnight) the rule produces from `start` on, in order; endless, the caller stops.
  // Weeks start on Monday (the iCalendar default). A monthly day that a month lacks (31st, 5th
  // Monday) and 29 February are skipped, as iCalendar does.
  static *_ruleDays(start, r) {
    const s0 = atMinutes(start, 0), iv = r.interval || 1;
    switch (r.freq) {
      case 'daily':
        for (let k = 0; ; k++) yield addDays(s0, k * iv);
      case 'weekly': {
        const days = r.byDay?.length ? r.byDay.map(c => WD.indexOf(c.slice(-2))) : [s0.getDay()];
        const offsets = [...new Set(days.map(w => (w + 6) % 7))].sort((a, b) => a - b);
        const monday = addDays(s0, -((s0.getDay() + 6) % 7));
        for (let k = 0; ; k++) {
          for (const o of offsets) {
            const d = addDays(monday, k * 7 * iv + o);
            if (d >= s0) yield d;
          }
        }
      }
      case 'monthly': {
        const specs = (r.byDay ?? []).map(c => ({ n: parseInt(c, 10) || 0, wd: WD.indexOf(c.slice(-2)) })).filter(x => x.n);
        for (let k = 0; ; k++) {
          const first = new Date(s0.getFullYear(), s0.getMonth() + k * iv, 1);
          const days = specs.length
            ? specs.map(sp => Agendo._nthWeekday(first, sp.n, sp.wd)).filter(Boolean).sort((a, b) => a - b)
            : [new Date(first.getFullYear(), first.getMonth(), s0.getDate())].filter(d => d.getDate() === s0.getDate());
          for (const d of days) if (d >= s0) yield d;
        }
      }
      case 'yearly':
        for (let k = 0; ; k++) {
          const d = new Date(s0.getFullYear() + k * iv, s0.getMonth(), s0.getDate());
          if (d.getMonth() === s0.getMonth()) yield d;
        }
    }
  }

  // The n-th (n < 0: from the end) weekday `wd` of the month starting on `first`, or null.
  static _nthWeekday(first, n, wd) {
    const y = first.getFullYear(), m = first.getMonth();
    const d = n > 0
      ? new Date(y, m, 1 + ((wd - first.getDay() + 7) % 7) + (n - 1) * 7)
      : (() => { const last = new Date(y, m + 1, 0); return new Date(y, m, last.getDate() - ((last.getDay() - wd + 7) % 7) + (n + 1) * 7); })();
    return d.getMonth() === m ? d : null;
  }

  // Occurrences of a recurring event overlapping [from, to): copies with the same time of day
  // (also across daylight-saving changes), id '<id>::YYYY-MM-DD', recurringId and occurrence.
  // `count` counts excluded dates too, as in iCalendar.
  static *_occurrencesOf(ev, from, to) {
    const r = ev.recurrence, until = r.until && parseDay(r.until), ex = new Set(r.exdates ?? []);
    const startMin = minutesOf(ev.start), endMin = minutesOf(ev.end), span = dayDiff(ev.start, ev.end);
    let n = 0, guard = 0;
    for (const day of Agendo._ruleDays(ev.start, r)) {
      if (++guard > 100000 || (until && day > until) || (r.count && n >= r.count)) break;
      n++;
      const start = atMinutes(day, startMin);
      if (start >= to) break;
      const key = dayKey(day);
      if (ex.has(key)) continue;
      const end = atMinutes(addDays(day, span), endMin);
      if (end > from || (+end === +start && start >= from)) {
        yield { ...ev, id: `${ev.id}::${key}`, recurringId: ev.id, occurrence: key, start, end };
      }
    }
  }

  // How many dates the rule produces before `key` (for splitting a series with a count).
  static _countBefore(ev, key) {
    let n = 0;
    for (const day of Agendo._ruleDays(ev.start, ev.recurrence)) {
      if (dayKey(day) >= key || n > 100000) break;
      n++;
    }
    return n;
  }

  // The series' rule, ending the day before `key`.
  static _endBefore(ev, key) {
    const r = { ...ev.recurrence };
    if (r.count) r.count = Math.max(1, Agendo._countBefore(ev, key));
    else r.until = dayKey(addDays(parseDay(key), -1));
    r.exdates = r.exdates?.filter(k => k < key);
    return Agendo._normRule(r);
  }

  // The rule with its weekdays moved by `days` (an occurrence dragged from Monday to Tuesday
  // with "All events" moves "every Monday" to "every Tuesday").
  static _shiftRule(r, days) {
    const out = { ...r };
    delete out.exdates;
    const n = ((days % 7) + 7) % 7;
    if (n && r.byDay) out.byDay = r.byDay.map(c => c.slice(0, -2) + WD[(WD.indexOf(c.slice(-2)) + n) % 7]);
    return out;
  }
}
