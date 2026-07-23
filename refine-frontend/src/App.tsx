import Router from '@/router';
import { Toaster } from 'sonner';

function App() {
  return (
    <>
      <Router />
      <Toaster position="top-right" />
    </>
  );
}

export default App;
