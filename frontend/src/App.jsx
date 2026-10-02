import { useCallback, useEffect, useState } from 'react'
import { createTestCase, deleteTestCase, getAllTestCases, getTestCases, updateTestCase } from './services/testCaseService'
import { createTestRun, deleteTestRun, getTestRuns, updateTestRun, updateTestRunDetails, updateTestRunStatus } from './services/testRunService'
import { getCurrentUser, login, logout } from './services/authService'
import { createProject, createUser, getUsers } from './services/adminService'
import logo from './assets/images/automate-it-mini-logo-dark.svg'
import './App.css'

const EMPTY_CASE = { name: '', method: 'GET', endpoint: '', description: '' }
const EMPTY_RUN = { name: '', testCaseId: '', environment: 'QA', executionType: 'SMOKE', notes: '' }
const EMPTY_DETAILS = { responseBody: '', evidence: '', logs: '', steps: [], assertions: [] }
const EMPTY_PROJECT = { name: '', slug: '' }
const EMPTY_USER = { username: '', displayName: '', password: '', role: 'VIEWER', projectIds: [] }
const EMPTY_PAGE = { content: [], page: { number: 0, size: 10, totalElements: 0, totalPages: 0 } }
const FINAL_STATUSES = new Set(['PASSED', 'FAILED', 'CANCELLED'])
const WRITE_ROLES = new Set(['ADMIN', 'MANAGER', 'TESTER'])
const ENVIRONMENTS = ['DEV', 'QA', 'STAGING', 'PROD_SIMULATED']
const EXECUTION_TYPES = ['SMOKE', 'REGRESSION', 'SANITY', 'API', 'UI']

function formatDate(value) { return value ? new Date(value).toLocaleString() : '—' }
function formatDuration(ms) { return ms == null ? '—' : ms < 1000 ? `${ms} ms` : `${(ms / 1000).toFixed(2)} s` }

function App() {
  const [authLoading, setAuthLoading] = useState(true)
  const [user, setUser] = useState(null)
  const [selectedProjectId, setSelectedProjectId] = useState('')
  const [activePage, setActivePage] = useState('dashboard')
  const [caseData, setCaseData] = useState(EMPTY_PAGE)
  const [caseCatalog, setCaseCatalog] = useState([])
  const [runData, setRunData] = useState(EMPTY_PAGE)
  const [resultData, setResultData] = useState(EMPTY_PAGE)
  const [casePage, setCasePage] = useState(0)
  const [runPage, setRunPage] = useState(0)
  const [resultPage, setResultPage] = useState(0)
  const [caseFilters, setCaseFilters] = useState({ q: '', method: '' })
  const [runFilters, setRunFilters] = useState({ q: '', status: '', environment: '', executionType: '', testCaseId: '' })
  const [caseForm, setCaseForm] = useState(null)
  const [runForm, setRunForm] = useState(null)
  const [detailsForm, setDetailsForm] = useState(null)
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    getCurrentUser().then((current) => {
      setUser(current); setSelectedProjectId(current.projects[0]?.id ?? '')
    }).catch((authError) => { if (authError.status !== 401) setError(authError.message) })
      .finally(() => setAuthLoading(false))
  }, [])

  const loadCases = useCallback(async () => {
    if (!selectedProjectId) return setCaseData(EMPTY_PAGE)
    const data = await getTestCases(selectedProjectId, { ...caseFilters, page: casePage, size: 10 })
    setCaseData(data)
  }, [selectedProjectId, caseFilters, casePage])

  const loadCaseCatalog = useCallback(async () => {
    if (!selectedProjectId) return setCaseCatalog([])
    setCaseCatalog(await getAllTestCases(selectedProjectId))
  }, [selectedProjectId])

  const loadRuns = useCallback(async () => {
    if (!selectedProjectId) return setRunData(EMPTY_PAGE)
    const data = await getTestRuns(selectedProjectId, { ...runFilters, page: runPage, size: 10 })
    setRunData(data)
  }, [selectedProjectId, runFilters, runPage])

  const loadResults = useCallback(async () => {
    if (!selectedProjectId) return setResultData(EMPTY_PAGE)
    const data = await getTestRuns(selectedProjectId, { finalOnly: true, page: resultPage, size: 10 })
    setResultData(data)
  }, [selectedProjectId, resultPage])

  const refreshAll = useCallback(async () => {
    if (!selectedProjectId) return
    setLoading(true); setError('')
    try { await Promise.all([loadCases(), loadCaseCatalog(), loadRuns(), loadResults()]) }
    catch (loadError) { setError(loadError.message) }
    finally { setLoading(false) }
  }, [selectedProjectId, loadCases, loadCaseCatalog, loadRuns, loadResults])

  useEffect(() => {
    if (!user || !selectedProjectId) return undefined
    let active = true
    Promise.resolve().then(() => Promise.all([
      getTestCases(selectedProjectId, { ...caseFilters, page: casePage, size: 10 }),
      getAllTestCases(selectedProjectId),
      getTestRuns(selectedProjectId, { ...runFilters, page: runPage, size: 10 }),
      getTestRuns(selectedProjectId, { finalOnly: true, page: resultPage, size: 10 }),
    ])).then(([cases, catalog, runs, results]) => {
      if (active) { setCaseData(cases); setCaseCatalog(catalog); setRunData(runs); setResultData(results) }
    }).catch((loadError) => { if (active) setError(loadError.message) })
    return () => { active = false }
  }, [user, selectedProjectId, caseFilters, runFilters, casePage, runPage, resultPage])

  useEffect(() => {
    if (user?.role === 'ADMIN' && activePage === 'administration') {
      getUsers().then(setUsers).catch((loadError) => setError(loadError.message))
    }
  }, [activePage, user])

  const selectedProject = user?.projects.find((project) => project.id === selectedProjectId)
  const canWrite = WRITE_ROLES.has(user?.role)
  const caseOptions = caseCatalog

  async function handleLogin(credentials) {
    setSaving(true); setError('')
    try {
      const authenticated = await login(credentials)
      setUser(authenticated); setSelectedProjectId(authenticated.projects[0]?.id ?? '')
    } catch (loginError) { setError(loginError.message) } finally { setSaving(false) }
  }

  async function handleLogout() {
    setSaving(true); try { await logout() } catch { /* clear local state */ }
    setUser(null); setSelectedProjectId(''); setSaving(false)
  }

  function openNewCase() { setActivePage('test-cases'); setCaseForm({ id: null, ...EMPTY_CASE }) }
  function openNewRun() { setActivePage('test-runs'); setRunForm({ id: null, ...EMPTY_RUN, testCaseId: caseOptions[0]?.id ?? '' }) }

  async function saveCase(event) {
    event.preventDefault(); setSaving(true); setError('')
    try {
      if (caseForm.id) await updateTestCase(caseForm.id, caseForm)
      else await createTestCase({ ...caseForm, projectId: selectedProjectId })
      setCaseForm(null); await Promise.all([loadCases(), loadCaseCatalog()])
    } catch (saveError) { setError(saveError.message) } finally { setSaving(false) }
  }

  async function removeCase(item) {
    if (!window.confirm(`Delete test case “${item.name}”?`)) return
    setSaving(true); setError('')
    try { await deleteTestCase(item.id); await Promise.all([loadCases(), loadCaseCatalog()]) }
    catch (deleteError) { setError(deleteError.message) }
    finally { setSaving(false) }
  }

  async function saveRun(event) {
    event.preventDefault(); setSaving(true); setError('')
    try {
      const payload = {
        projectId: selectedProjectId,
        testCaseId: runForm.testCaseId,
        name: runForm.name,
        environment: runForm.environment,
        executionType: runForm.executionType,
        notes: runForm.notes,
      }
      if (runForm.id) await updateTestRun(runForm.id, payload)
      else await createTestRun(payload)
      setRunForm(null); await Promise.all([loadRuns(), loadResults()])
    } catch (saveError) { setError(saveError.message) } finally { setSaving(false) }
  }

  async function saveDetails(event) {
    event.preventDefault(); setSaving(true); setError('')
    try {
      const payload = {
        responseBody: detailsForm.responseBody,
        evidence: detailsForm.evidence,
        logs: detailsForm.logs,
        steps: detailsForm.steps.map(({ description, expectedResult, actualResult, status }) => ({ description, expectedResult, actualResult, status })),
        assertions: detailsForm.assertions.map(({ name, expectedValue, actualValue, passed }) => ({ name, expectedValue, actualValue, passed })),
      }
      await updateTestRunDetails(detailsForm.id, payload)
      setDetailsForm(null); await Promise.all([loadRuns(), loadResults()])
    } catch (saveError) { setError(saveError.message) } finally { setSaving(false) }
  }

  async function changeStatus(run, status) {
    setSaving(true); setError('')
    try { await updateTestRunStatus(run.id, status); await Promise.all([loadRuns(), loadResults()]) }
    catch (updateError) { setError(updateError.message) }
    finally { setSaving(false) }
  }

  async function removeRun(run) {
    if (!window.confirm(`Delete test run “${run.name}” and all stored evidence?`)) return
    setSaving(true); setError('')
    try { await deleteTestRun(run.id); await Promise.all([loadRuns(), loadResults()]) }
    catch (deleteError) { setError(deleteError.message) }
    finally { setSaving(false) }
  }

  async function handleCreateProject(project) {
    setSaving(true); setError('')
    try {
      const created = await createProject(project); const refreshed = await getCurrentUser()
      setUser(refreshed); setSelectedProjectId(created.id); return true
    } catch (createError) { setError(createError.message); return false } finally { setSaving(false) }
  }

  async function handleCreateUser(account) {
    setSaving(true); setError('')
    try {
      const created = await createUser(account)
      setUsers((current) => [...current, created].sort((a, b) => a.username.localeCompare(b.username))); return true
    } catch (createError) { setError(createError.message); return false } finally { setSaving(false) }
  }

  if (authLoading) return <div className="auth-screen"><div className="loading-state">Checking session…</div></div>
  if (!user) return <LoginPage saving={saving} error={error} onLogin={handleLogin} onClearError={() => setError('')} />

  const navItems = [['dashboard', 'Dashboard'], ['test-cases', 'Test Cases'], ['test-runs', 'Test Runs'], ['results', 'Results'],
    ...(user.role === 'ADMIN' ? [['administration', 'Administration']] : [])]
  const passedOnPage = resultData.content.filter((run) => run.status === 'PASSED').length
  const failedOnPage = resultData.content.filter((run) => run.status === 'FAILED').length

  return <div className="app"><aside className="sidebar"><div className="logo"><img src={logo} alt="Automate-it Mini" className="logo-image" /></div>
    <nav>{navItems.map(([page, label]) => <button key={page} className={`nav-item ${activePage === page ? 'active' : ''}`} onClick={() => setActivePage(page)}>{label}</button>)}</nav>
    <div className="account-panel"><strong>{user.displayName}</strong><span>@{user.username} · {user.role}</span><button onClick={handleLogout}>Sign out</button></div></aside>
    <main className="main-content"><div className="project-toolbar"><label><span>Active project</span><select value={selectedProjectId} onChange={(e) => { setSelectedProjectId(e.target.value); setCasePage(0); setRunPage(0); setResultPage(0) }}>{user.projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}</select></label>{selectedProject && <span className="project-slug">/{selectedProject.slug}</span>}</div>
      {error && <div className="error-banner"><span>{error}</span><button onClick={() => setError('')}>×</button></div>}
      {!selectedProject && activePage !== 'administration' ? <EmptyState title="No project access" text="Ask an administrator to assign a project." /> : loading ? <div className="loading-state">Loading data…</div> : <>
        {activePage === 'dashboard' && <Dashboard caseTotal={caseData.page.totalElements} runTotal={runData.page.totalElements} resultTotal={resultData.page.totalElements} passed={passedOnPage} failed={failedOnPage} canWrite={canWrite} recentRuns={runData.content.slice(0, 5)} onNewCase={openNewCase} onNewRun={openNewRun} onViewRuns={() => setActivePage('test-runs')} />}
        {activePage === 'test-cases' && <TestCasesPage data={caseData} filters={caseFilters} setFilters={(value) => { setCasePage(0); setCaseFilters(value) }} page={casePage} setPage={setCasePage} form={caseForm} setForm={setCaseForm} saving={saving} canWrite={canWrite} onNew={openNewCase} onSave={saveCase} onDelete={removeCase} />}
        {activePage === 'test-runs' && <TestRunsPage data={runData} testCases={caseOptions} filters={runFilters} setFilters={(value) => { setRunPage(0); setRunFilters(value) }} page={runPage} setPage={setRunPage} form={runForm} setForm={setRunForm} detailsForm={detailsForm} setDetailsForm={setDetailsForm} saving={saving} canWrite={canWrite} onNew={openNewRun} onSave={saveRun} onSaveDetails={saveDetails} onStatus={changeStatus} onDelete={removeRun} onViewResult={() => setActivePage('results')} />}
        {activePage === 'results' && <ResultsPage data={resultData} page={resultPage} setPage={setResultPage} canWrite={canWrite} onNew={openNewRun} />}
        {activePage === 'administration' && <AdministrationPage projects={user.projects} users={users} saving={saving} onCreateProject={handleCreateProject} onCreateUser={handleCreateUser} />}
      </>}
      {selectedProject && activePage !== 'administration' && <button className="refresh-button" onClick={refreshAll}>Refresh data</button>}
    </main></div>
}

function LoginPage({ saving, error, onLogin, onClearError }) {
  const [form, setForm] = useState({ username: '', password: '' })
  return <div className="auth-screen"><section className="login-card"><img src={logo} alt="Automate-it Mini" className="login-logo" /><p className="subtitle">Secure workspace</p><h1>Sign in</h1>
    {error && <div className="error-banner"><span>{error}</span><button onClick={onClearError}>×</button></div>}
    <form onSubmit={(e) => { e.preventDefault(); onLogin(form) }}><FormField label="Username"><input autoFocus required value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} /></FormField><FormField label="Password"><input required type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></FormField><button className="primary-button login-button" disabled={saving}>Sign in</button></form>
    <small>Credentials are managed by the administrator and never displayed.</small></section></div>
}

function PageHeader({ eyebrow, title, buttonLabel, onButtonClick }) { return <header className="topbar"><div><p className="subtitle">{eyebrow}</p><h1>{title}</h1></div>{buttonLabel && <button className="new-test-button" onClick={onButtonClick}>{buttonLabel}</button>}</header> }

function Dashboard({ caseTotal, runTotal, resultTotal, passed, failed, canWrite, recentRuns, onNewCase, onNewRun, onViewRuns }) { return <>
  <PageHeader eyebrow="Testing Dashboard" title="Welcome to Automate-it Mini" buttonLabel={canWrite ? '+ New Test' : null} onButtonClick={onNewCase} />
  <section className="cards"><div className="card"><p>Total Test Cases</p><h2>{caseTotal}</h2></div><div className="card"><p>Filtered Test Runs</p><h2>{runTotal}</h2></div><div className="card"><p>Completed</p><h2>{resultTotal}</h2></div><div className="card"><p>Passed / Failed (page)</p><h2>{passed} / {failed}</h2></div></section>
  <section className="recent-tests"><div className="section-header"><div><p className="subtitle">Execution history</p><h2>Recent Test Runs</h2></div>{recentRuns.length > 0 && <button className="text-button" onClick={onViewRuns}>View all</button>}</div>{recentRuns.length ? <RunList runs={recentRuns} /> : <EmptyState title="No test runs" text="Create an execution linked to a test case." button={canWrite ? 'Create run' : null} onClick={onNewRun} />}</section></> }

function TestCasesPage({ data, filters, setFilters, page, setPage, form, setForm, saving, canWrite, onNew, onSave, onDelete }) { return <>
  <PageHeader eyebrow="Test Management" title="Test Cases" buttonLabel={canWrite ? '+ New Test Case' : null} onButtonClick={onNew} />
  <FilterBar><input placeholder="Search name, endpoint or description" value={filters.q} onChange={(e) => setFilters({ ...filters, q: e.target.value })} /><select value={filters.method} onChange={(e) => setFilters({ ...filters, method: e.target.value })}><option value="">All methods</option>{['GET','POST','PUT','PATCH','DELETE'].map((m) => <option key={m}>{m}</option>)}</select><button onClick={() => setFilters({ q: '', method: '' })}>Clear</button></FilterBar>
  {form && <form className="test-form" onSubmit={onSave}><FormHeader eyebrow={form.id ? 'Edit definition' : 'New automated test'} title={form.id ? 'Edit Test Case' : 'Create Test Case'} onClose={() => setForm(null)} /><div className="form-grid"><FormField label="Name"><input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></FormField><FormField label="Method"><select value={form.method} onChange={(e) => setForm({ ...form, method: e.target.value })}>{['GET','POST','PUT','PATCH','DELETE'].map((m) => <option key={m}>{m}</option>)}</select></FormField><FormField label="Endpoint" fullWidth><input required pattern="/.*" value={form.endpoint} onChange={(e) => setForm({ ...form, endpoint: e.target.value })} /></FormField><FormField label="Description" fullWidth><textarea rows="3" value={form.description ?? ''} onChange={(e) => setForm({ ...form, description: e.target.value })} /></FormField></div><FormActions saving={saving} onCancel={() => setForm(null)} saveLabel={form.id ? 'Update Test Case' : 'Save Test Case'} /></form>}
  <section className="recent-tests"><div className="item-list">{data.content.map((item) => <article className="list-card" key={item.id}><div><h3>{item.name}</h3><p>{item.description || 'No description.'}</p><div className="endpoint"><strong>{item.method}</strong><code>{item.endpoint}</code></div></div>{canWrite && <div className="row-actions"><button onClick={() => setForm({ ...item })}>Edit</button><button className="danger-button" onClick={() => onDelete(item)}>Delete</button></div>}</article>)}</div>{!data.content.length && <EmptyState title="No matching cases" text="Adjust filters or create a test case." />}
    <Pagination page={data.page} current={page} onChange={setPage} /></section></> }

function TestRunsPage({ data, testCases, filters, setFilters, page, setPage, form, setForm, detailsForm, setDetailsForm, saving, canWrite, onNew, onSave, onSaveDetails, onStatus, onDelete, onViewResult }) { return <>
  <PageHeader eyebrow="Execution Management" title="Test Runs" buttonLabel={canWrite && testCases.length ? '+ New Test Run' : null} onButtonClick={onNew} />
  <FilterBar><input placeholder="Search runs, cases, notes or author" value={filters.q} onChange={(e) => setFilters({ ...filters, q: e.target.value })} /><select value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}><option value="">All statuses</option>{['PENDING','RUNNING','PASSED','FAILED','CANCELLED'].map((v) => <option key={v}>{v}</option>)}</select><select value={filters.environment} onChange={(e) => setFilters({ ...filters, environment: e.target.value })}><option value="">All environments</option>{ENVIRONMENTS.map((v) => <option key={v}>{v}</option>)}</select><select value={filters.executionType} onChange={(e) => setFilters({ ...filters, executionType: e.target.value })}><option value="">All types</option>{EXECUTION_TYPES.map((v) => <option key={v}>{v}</option>)}</select><select value={filters.testCaseId} onChange={(e) => setFilters({ ...filters, testCaseId: e.target.value })}><option value="">All test cases</option>{testCases.map((v) => <option key={v.id} value={v.id}>{v.name}</option>)}</select><button onClick={() => setFilters({ q: '', status: '', environment: '', executionType: '', testCaseId: '' })}>Clear</button></FilterBar>
  {!testCases.length && <div className="info-banner">Create a Test Case before creating a Test Run.</div>}
  {form && <RunForm form={form} setForm={setForm} testCases={testCases} saving={saving} onSave={onSave} />}
  {detailsForm && <ExecutionDetailsForm form={detailsForm} setForm={setDetailsForm} saving={saving} onSave={onSaveDetails} />}
  <section className="recent-tests"><RunList runs={data.content} saving={saving} canWrite={canWrite} onStatus={onStatus} onEdit={(run) => setForm({ ...run })} onDetails={(run) => setDetailsForm({ id: run.id, responseBody: run.responseBody ?? '', evidence: run.evidence ?? '', logs: run.logs ?? '', steps: run.steps ?? [], assertions: run.assertions ?? [] })} onDelete={onDelete} onViewResult={onViewResult} />{!data.content.length && <EmptyState title="No matching runs" text="Adjust the filters or create an execution." />}<Pagination page={data.page} current={page} onChange={setPage} /></section></> }

function RunForm({ form, setForm, testCases, saving, onSave }) { return <form className="test-form" onSubmit={onSave}><FormHeader eyebrow={form.id ? 'Edit execution' : 'New execution'} title={form.id ? 'Edit Test Run' : 'Create Test Run'} onClose={() => setForm(null)} /><div className="form-grid"><FormField label="Run name"><input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></FormField><FormField label="Test Case"><select required value={form.testCaseId} onChange={(e) => setForm({ ...form, testCaseId: e.target.value })}><option value="">Select a test case</option>{testCases.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></FormField><FormField label="Environment"><select value={form.environment} onChange={(e) => setForm({ ...form, environment: e.target.value })}>{ENVIRONMENTS.map((v) => <option key={v}>{v}</option>)}</select></FormField><FormField label="Execution type"><select value={form.executionType} onChange={(e) => setForm({ ...form, executionType: e.target.value })}>{EXECUTION_TYPES.map((v) => <option key={v}>{v}</option>)}</select></FormField><FormField label="Notes" fullWidth><textarea rows="3" value={form.notes ?? ''} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></FormField></div><FormActions saving={saving} onCancel={() => setForm(null)} saveLabel={form.id ? 'Update Test Run' : 'Create Test Run'} /></form> }

function ExecutionDetailsForm({ form, setForm, saving, onSave }) {
  const addStep = () => setForm({ ...form, steps: [...form.steps, { description: '', expectedResult: '', actualResult: '', status: 'PENDING' }] })
  const addAssertion = () => setForm({ ...form, assertions: [...form.assertions, { name: '', expectedValue: '', actualValue: '', passed: false }] })
  return <form className="test-form details-form" onSubmit={onSave}><FormHeader eyebrow="Execution evidence" title="Steps, Assertions and Output" onClose={() => setForm(null)} /><div className="form-grid"><FormField label="Response body" fullWidth><textarea rows="5" value={form.responseBody} onChange={(e) => setForm({ ...form, responseBody: e.target.value })} /></FormField><FormField label="Evidence (links or notes)" fullWidth><textarea rows="3" value={form.evidence} onChange={(e) => setForm({ ...form, evidence: e.target.value })} /></FormField><FormField label="Logs" fullWidth><textarea rows="5" value={form.logs} onChange={(e) => setForm({ ...form, logs: e.target.value })} /></FormField></div>
    <CollectionEditor title="Steps" onAdd={addStep}>{form.steps.map((step, index) => <div className="detail-row" key={index}><input required placeholder="Step description" value={step.description} onChange={(e) => setForm({ ...form, steps: form.steps.map((v, i) => i === index ? { ...v, description: e.target.value } : v) })} /><input placeholder="Expected" value={step.expectedResult ?? ''} onChange={(e) => setForm({ ...form, steps: form.steps.map((v, i) => i === index ? { ...v, expectedResult: e.target.value } : v) })} /><input placeholder="Actual" value={step.actualResult ?? ''} onChange={(e) => setForm({ ...form, steps: form.steps.map((v, i) => i === index ? { ...v, actualResult: e.target.value } : v) })} /><select value={step.status} onChange={(e) => setForm({ ...form, steps: form.steps.map((v, i) => i === index ? { ...v, status: e.target.value } : v) })}>{['PENDING','PASSED','FAILED','SKIPPED'].map((v) => <option key={v}>{v}</option>)}</select><button type="button" onClick={() => setForm({ ...form, steps: form.steps.filter((_, i) => i !== index) })}>Remove</button></div>)}</CollectionEditor>
    <CollectionEditor title="Assertions" onAdd={addAssertion}>{form.assertions.map((item, index) => <div className="detail-row assertion-row" key={index}><input required placeholder="Assertion" value={item.name} onChange={(e) => setForm({ ...form, assertions: form.assertions.map((v, i) => i === index ? { ...v, name: e.target.value } : v) })} /><input placeholder="Expected" value={item.expectedValue ?? ''} onChange={(e) => setForm({ ...form, assertions: form.assertions.map((v, i) => i === index ? { ...v, expectedValue: e.target.value } : v) })} /><input placeholder="Actual" value={item.actualValue ?? ''} onChange={(e) => setForm({ ...form, assertions: form.assertions.map((v, i) => i === index ? { ...v, actualValue: e.target.value } : v) })} /><label><input type="checkbox" checked={item.passed} onChange={(e) => setForm({ ...form, assertions: form.assertions.map((v, i) => i === index ? { ...v, passed: e.target.checked } : v) })} /> Passed</label><button type="button" onClick={() => setForm({ ...form, assertions: form.assertions.filter((_, i) => i !== index) })}>Remove</button></div>)}</CollectionEditor><FormActions saving={saving} onCancel={() => setForm(null)} saveLabel="Save execution details" /></form>
}

function CollectionEditor({ title, onAdd, children }) { return <section className="collection-editor"><div className="section-header"><h3>{title}</h3><button type="button" onClick={onAdd}>+ Add</button></div>{children}</section> }

function RunList({ runs, saving, canWrite, onStatus, onEdit, onDetails, onDelete, onViewResult }) { return <div className="item-list">{runs.map((run) => <article className="list-card run-card" key={run.id}><div><div className="run-title"><h3>{run.name}</h3><StatusBadge status={run.status} /></div><p><strong>{run.testCaseName}</strong> · {run.environment} · {run.executionType}</p><small>Created {formatDate(run.createdAt)} by {run.createdBy} · Duration {formatDuration(run.durationMs)}</small></div><div className="run-actions">{canWrite && run.status === 'PENDING' && <button disabled={saving} onClick={() => onStatus(run, 'RUNNING')}>Start</button>}{canWrite && run.status === 'RUNNING' && <button className="success-button" onClick={() => onStatus(run, 'PASSED')}>Pass</button>}{canWrite && run.status === 'RUNNING' && <button className="danger-button" onClick={() => onStatus(run, 'FAILED')}>Fail</button>}{canWrite && !FINAL_STATUSES.has(run.status) && <button className="secondary-button compact" onClick={() => onStatus(run, 'CANCELLED')}>Cancel</button>}{canWrite && !FINAL_STATUSES.has(run.status) && onEdit && <button onClick={() => onEdit(run)}>Edit</button>}{canWrite && onDetails && <button onClick={() => onDetails(run)}>Details</button>}{FINAL_STATUSES.has(run.status) && onViewResult && <button onClick={onViewResult}>View result</button>}{canWrite && onDelete && <button className="danger-button" onClick={() => onDelete(run)}>Delete</button>}</div></article>)}</div> }

function ResultsPage({ data, page, setPage, canWrite, onNew }) { return <><PageHeader eyebrow="Execution Results" title="Results" buttonLabel={canWrite ? '+ New Test Run' : null} onButtonClick={onNew} /><section className="recent-tests">{data.content.length ? <div className="results-grid">{data.content.map((run) => <article className={`result-card result-${run.status.toLowerCase()}`} key={run.id}><div className="result-heading"><div><h3>{run.name}</h3><small>{run.testCaseName}</small></div><StatusBadge status={run.status} /></div><dl><div><dt>Environment</dt><dd>{run.environment}</dd></div><div><dt>Duration</dt><dd>{formatDuration(run.durationMs)}</dd></div><div><dt>Started</dt><dd>{formatDate(run.startedAt)}</dd></div><div><dt>Finished</dt><dd>{formatDate(run.finishedAt)}</dd></div></dl><ResultBlock label="Response body" value={run.responseBody} /><ResultBlock label="Evidence" value={run.evidence} /><ResultBlock label="Logs" value={run.logs} />{run.steps?.length > 0 && <div className="result-details"><strong>Steps</strong>{run.steps.map((step) => <p key={step.id}>{step.orderIndex}. {step.description} — {step.status}</p>)}</div>}{run.assertions?.length > 0 && <div className="result-details"><strong>Assertions</strong>{run.assertions.map((item) => <p key={item.id}>{item.passed ? '✓' : '✕'} {item.name}: {item.actualValue || '—'}</p>)}</div>}</article>)}</div> : <EmptyState title="No completed results" text="Finish a run to see its result and evidence." />}<Pagination page={data.page} current={page} onChange={setPage} /></section></> }
function ResultBlock({ label, value }) { return value ? <div className="result-details"><strong>{label}</strong><pre>{value}</pre></div> : null }

function AdministrationPage({ projects, users, saving, onCreateProject, onCreateUser }) {
  const [projectForm, setProjectForm] = useState(EMPTY_PROJECT); const [userForm, setUserForm] = useState(EMPTY_USER)
  return <><PageHeader eyebrow="Access Control" title="Administration" /><div className="admin-grid"><form className="test-form" onSubmit={async (e) => { e.preventDefault(); if (await onCreateProject(projectForm)) setProjectForm(EMPTY_PROJECT) }}><FormHeader eyebrow="Project segregation" title="Create Project" /><FormField label="Project name"><input required value={projectForm.name} onChange={(e) => setProjectForm({ name: e.target.value, slug: e.target.value.toLowerCase().trim().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') })} /></FormField><FormField label="Slug"><input required value={projectForm.slug} onChange={(e) => setProjectForm({ ...projectForm, slug: e.target.value })} /></FormField><button className="primary-button">Create Project</button></form><form className="test-form" onSubmit={async (e) => { e.preventDefault(); if (await onCreateUser(userForm)) setUserForm(EMPTY_USER) }}><FormHeader eyebrow="Persisted identity" title="Create User" /><div className="form-grid"><FormField label="Username"><input required value={userForm.username} onChange={(e) => setUserForm({ ...userForm, username: e.target.value })} /></FormField><FormField label="Display name"><input required value={userForm.displayName} onChange={(e) => setUserForm({ ...userForm, displayName: e.target.value })} /></FormField><FormField label="Password"><input required type="password" minLength="12" pattern="(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+" value={userForm.password} onChange={(e) => setUserForm({ ...userForm, password: e.target.value })} /></FormField><FormField label="Role"><select value={userForm.role} onChange={(e) => setUserForm({ ...userForm, role: e.target.value })}>{['ADMIN','MANAGER','TESTER','VIEWER'].map((v) => <option key={v}>{v}</option>)}</select></FormField><FormField label="Assigned projects" fullWidth><div className="project-checkboxes">{projects.map((project) => <label key={project.id}><input type="checkbox" checked={userForm.projectIds.includes(project.id)} onChange={(e) => setUserForm({ ...userForm, projectIds: e.target.checked ? [...userForm.projectIds, project.id] : userForm.projectIds.filter((id) => id !== project.id) })} />{project.name}</label>)}</div></FormField></div><button className="primary-button" disabled={saving}>Create User</button></form></div><section className="recent-tests"><div className="item-list">{users.map((account) => <article className="list-card" key={account.id}><div><h3>{account.displayName}</h3><p>@{account.username} · {account.projects.map((p) => p.name).join(', ') || 'All projects'}</p></div><StatusBadge status={account.role} /></article>)}</div></section></>
}

function FilterBar({ children }) { return <div className="filter-bar">{children}</div> }
function Pagination({ page, current, onChange }) { if (!page || page.totalPages <= 1) return null; return <div className="pagination"><button disabled={current <= 0} onClick={() => onChange(current - 1)}>Previous</button><span>Page {current + 1} of {page.totalPages} · {page.totalElements} items</span><button disabled={current + 1 >= page.totalPages} onClick={() => onChange(current + 1)}>Next</button></div> }
function StatusBadge({ status }) { return <span className={`status-badge status-${status.toLowerCase()}`}>{status}</span> }
function EmptyState({ title, text, button, onClick }) { return <div className="empty-state"><div className="empty-icon">✦</div><h3>{title}</h3><p>{text}</p>{button && <button className="primary-button" onClick={onClick}>{button}</button>}</div> }
function FormHeader({ eyebrow, title, onClose }) { return <div className="form-header"><div><p className="subtitle">{eyebrow}</p><h2>{title}</h2></div>{onClose && <button type="button" className="close-button" onClick={onClose}>✕</button>}</div> }
function FormField({ label, fullWidth = false, children }) { return <label className={`form-group ${fullWidth ? 'full-width' : ''}`}><span>{label}</span>{children}</label> }
function FormActions({ saving, onCancel, saveLabel }) { return <div className="form-actions"><button type="button" className="secondary-button" onClick={onCancel}>Cancel</button><button type="submit" className="primary-button" disabled={saving}>{saving ? 'Saving…' : saveLabel}</button></div> }

export default App
