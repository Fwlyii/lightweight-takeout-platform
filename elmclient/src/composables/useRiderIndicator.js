import { onMounted, onUnmounted, onUpdated } from 'vue';

// Measure the existing buttons; routing and tab selection remain owned by the page.
export function useRiderIndicator(element) {
    let observer;
    const update = () => {
        const root = element.value;
        const active = root?.querySelector('.active');
        if (!active) return;
        root.style.setProperty('--indicator-x', `${active.offsetLeft}px`);
        root.style.setProperty('--indicator-width', `${active.offsetWidth}px`);
        root.style.setProperty('--indicator-visible', '1');
    };
    onMounted(() => {
        update();
        if (typeof ResizeObserver !== 'undefined') {
            observer = new ResizeObserver(update);
            if (element.value) observer.observe(element.value);
        }
    });
    onUpdated(() => {
        if (element.value) observer?.observe(element.value);
        update();
    });
    onUnmounted(() => observer?.disconnect());
}
