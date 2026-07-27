import React from 'react';

interface HeroBannerProps {
  title: string;
  imageUrl: string;
}

export function HeroBanner({ title, imageUrl }: HeroBannerProps) {
  return (
    <div
      className="relative bg-cover bg-center h-64 flex items-center justify-center text-white"
      style={{ backgroundImage: `url(${imageUrl})` }}
    >
      <div className="absolute inset-0 bg-black/40" />
      <h1 className="relative text-4xl font-bold">{title}</h1>
    </div>
  );
}
