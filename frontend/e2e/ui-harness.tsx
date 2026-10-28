import { createRoot } from "react-dom/client";
import { useState } from "react";
import { Button, Card, Checkbox, EmptyState, ErrorState, Field, Input, LoadingIndicator, MetricCard, PageHeader, Select, Skeleton, StatusBadge, Textarea } from "../src/components/ui";
import { Dialog } from "../src/components/ui/Dialog";
import { Tabs, Tooltip } from "../src/components/ui/Disclosure";
import { ReorderGroup, UpdatedValue } from "../src/components/ui/Motion";
import { PlayerAvatar, PlayerIdentity } from "../src/components/players/PlayerAvatar";

// Bundled only by the test runner into tmp/. Never adds an application route.
function Harness() {
  const [tab, setTab] = useState("identity"); const [open, setOpen] = useState(false);
  const [url, setUrl] = useState("/avatar-valid.svg"); const [order, setOrder] = useState(["Ananya Deshmukh", "Meera Rao"]);
  return <main className="mx-auto max-w-5xl p-6"><PageHeader title="Shared interface foundation" eyebrow="Play-Neml component review" actions={<Button onClick={() => setOpen(true)}>Open review dialog</Button>} />
    <div className="grid gap-4 sm:grid-cols-3"><MetricCard label="Registered players" value="24" detail="Tabular numeral sample" /><Card><h2 className="section-title mb-4">Status</h2><div className="flex flex-wrap gap-2">{(["neutral", "success", "warning", "danger", "info", "live"] as const).map(tone => <StatusBadge key={tone} tone={tone} live={tone === "live"}>{tone}</StatusBadge>)}</div></Card><Card><Skeleton className="h-5 w-24" /><LoadingIndicator label="Loading records…" /></Card></div>
    <Card className="mt-4"><Tabs label="Component review" value={tab} onChange={setTab} tabs={[
      { value: "identity", label: "Player identity", content: <div className="space-y-5"><div className="flex flex-wrap items-center gap-4"><PlayerAvatar /><PlayerAvatar name="Ananya Deshmukh" size="sm" /><PlayerAvatar name="Meera Rao" size="lg" shape="square" /><PlayerAvatar name="Vikram Nair" size="xl" /><PlayerAvatar name="Loading player" loading /></div><PlayerIdentity name="Ishita Patil" metadata="Badminton · Advanced" /><div className="flex items-center gap-3"><PlayerAvatar name="Photo player" imageUrl={url} alt="Photo preview" /><Button variant="ghost" onClick={() => setUrl(url === "/avatar-valid.svg" ? "/avatar-broken.svg" : "/avatar-valid.svg")}>Change avatar URL</Button></div></div> },
      { value: "forms", label: "Form controls", content: <div className="grid gap-4 sm:grid-cols-2"><Field label="Team name" hint="Use a recognizable name.">{props => <Input {...props} placeholder="Team name" />}</Field><Field label="Invalid entry" error="A value is required.">{props => <Input {...props} />}</Field><Field label="Sport">{props => <Select {...props}><option>Badminton</option><option>Cricket</option></Select>}</Field><Field label="Notes">{props => <Textarea {...props} />}</Field><Checkbox label="Confirm squad" /><Input disabled aria-label="Unavailable control" value="Disabled" readOnly /></div> },
      { value: "feedback", label: "Feedback", content: <div><div className="flex flex-wrap gap-3"><Button loading>Saving</Button><Button disabled>Unavailable</Button><Button variant="danger">Danger</Button><Tooltip label="More information"><span>Focus for help</span></Tooltip></div><EmptyState title="No fixtures yet" description="Scheduled fixtures will appear here." /><ErrorState description="The request could not be completed." onRetry={() => {}} /></div> },
    ]} /></Card>
    <Card className="mt-4"><Button variant="ghost" onClick={() => setOrder([...order].reverse())}>Reorder players</Button><UpdatedValue revision={order[0]}>{order[0]}</UpdatedValue><ReorderGroup revision={order[0]} className="mt-4 space-y-3">{order.map(name => <div key={name} data-motion-id={name} className="rounded-lg border border-border p-3"><PlayerIdentity name={name} /></div>)}</ReorderGroup></Card>
    {open && <Dialog title="Review dialog" description="Keyboard accessible modal" onClose={() => setOpen(false)}><Field label="Review note">{props => <Input {...props} />}</Field><Button className="mt-4" onClick={() => setOpen(false)}>Done</Button></Dialog>}
  </main>;
}
createRoot(document.getElementById("root")!).render(<Harness />);
