import { Card, EmptyState, PageHeader } from "@/components/ui";
export default function ComingSoonPage({ title, subtitle }: { title: string; subtitle: string }) {
  return <div className="portal-page"><PageHeader title={title} description={subtitle} eyebrow="Championship information" /><Card><EmptyState title="No announcement feed is connected" description="This platform does not yet provide an announcements publishing API. Use the fixtures, results and live auction views for current competition information." /></Card></div>;
}
