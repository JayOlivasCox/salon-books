import { SymbolView } from 'expo-symbols';
import { Tabs } from 'expo-router/js-tabs';

import { C } from '@/lib/theme';

export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: C.toner,
        tabBarInactiveTintColor: C.inkFaint,
        tabBarStyle: { backgroundColor: C.surface, borderTopColor: C.line },
        sceneStyle: { backgroundColor: C.bg },
      }}>
      <Tabs.Screen name="index" options={{
        title: 'Today',
        tabBarIcon: ({ color }) => <SymbolView name="scissors" size={24} tintColor={color} />,
      }} />
      <Tabs.Screen name="expenses" options={{
        title: 'Expenses',
        tabBarIcon: ({ color }) => <SymbolView name="receipt" size={24} tintColor={color} />,
      }} />
      <Tabs.Screen name="reports" options={{
        title: 'Reports',
        tabBarIcon: ({ color }) => <SymbolView name="chart.bar" size={24} tintColor={color} />,
      }} />
      <Tabs.Screen name="settings" options={{
        title: 'Settings',
        tabBarIcon: ({ color }) => <SymbolView name="gearshape" size={24} tintColor={color} />,
      }} />
    </Tabs>
  );
}
